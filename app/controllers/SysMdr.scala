package controllers

import com.google.inject.Inject
import io.suggest.common.fut.FutureUtil
import io.suggest.di.IEsClient
import io.suggest.event.SioNotifierStaticClientI
import io.suggest.model.n2.edge.{MNodeEdges, MEdgeInfo}
import models.mdr._
import models.msys.MSysMdrFreeAdvsTplArgs
import org.elasticsearch.client.Client
import org.joda.time.DateTime
import play.api.i18n.MessagesApi
import play.api.mvc.Result
import util.PlayMacroLogsImpl
import util.acl.{RequestWithAd, IsSuperuserMad, IsSuperuser}
import util.lk.LkAdUtil
import util.n2u.N2NodesUtil
import util.showcase.ShowcaseUtil
import views.html.sys1.mdr._
import models._
import play.api.data.Form

import scala.concurrent.{ExecutionContext, Future}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 19.06.14 10:45
 * Description: Sys Moderation - контроллер, заправляющий s.io-модерацией рекламных карточек.
 */
class SysMdr @Inject() (
  lkAdUtil                          : LkAdUtil,
  scUtil                            : ShowcaseUtil,
  mNodeCache                        : MAdnNodeCache,
  n2NodesUtil                       : N2NodesUtil,
  override val _contextFactory      : Context2Factory,
  override val messagesApi          : MessagesApi,
  override implicit val ec          : ExecutionContext,
  override implicit val esClient    : Client,
  override implicit val sn          : SioNotifierStaticClientI
)
  extends SioControllerImpl
  with PlayMacroLogsImpl
  with IEsClient
  with IsSuperuser
  with IsSuperuserMad
{

  import LOGGER._

  /** Отобразить начальную страницу раздела модерации рекламных карточек. */
  def index = IsSuperuser { implicit request =>
    Ok( mdrIndexTpl() )
  }

  /** Страница с бесплатно-размещёнными рекламными карточками, подлежащими модерации s.io.
    * @param args Аргументы для поиска (QSB).
    */
  def freeAdvs(args: MdrSearchArgs) = IsSuperuser.async { implicit request =>
    var madsFut: Future[Seq[MNode]] = {
      MNode.dynSearch( args.toNodeSearch )
    }
    for (hai <- args.hideAdIdOpt) {
      madsFut = madsFut.map { mads0 =>
        mads0.filter(_.id.get != hai)
      }
    }
    val producersFut = madsFut flatMap { mads =>
      // Сгребаем всех продьюсеров карточек + добавляем запрошенных продьюсеров, дедублицируем список.
      val prodIds = mads.iterator
        .flatMap { n2NodesUtil.madProducerId }
        .++( args.producerId )
        .toSet
      mNodeCache.multiGet( prodIds )
    }
    val prodsMapFut = producersFut map { prods =>
      prods
        .map { p => p.id.get -> p }
        .toMap
    }

    val brArgssFut = madsFut flatMap { mads =>
      Future.traverse(mads) { mad =>
        lkAdUtil.tiledAdBrArgs(mad)
      }
    }

    val prodOptFut = FutureUtil.optFut2futOpt( args.producerId ) { prodId =>
      prodsMapFut map { prodsMap =>
        prodsMap.get(prodId)
      }
    }

    for {
      brArgss   <- brArgssFut
      prodsMap  <- prodsMapFut
      mnodeOpt  <- prodOptFut
    } yield {
      val rargs = MSysMdrFreeAdvsTplArgs(
        args0     = args,
        mads      = brArgss,
        prodsMap  = prodsMap,
        producerOpt  = mnodeOpt
      )
      Ok( freeAdvsTpl(rargs) )
    }
  }


  private def banFreeAdvFormM = {
    import play.api.data._, Forms._
    import util.FormUtil._
    Form(
      "reason" -> nonEmptyText(minLength = 4, maxLength = 1024)
        .transform(strTrimSanitizeF, strIdentityF)
    )
  }

  /** Страница для модерации одной карточки. */
  def freeAdvMdr(adId: String) = IsSuperuserMad(adId).async { implicit request =>
    freeAdvMdrBody(banFreeAdvFormM, Ok)
  }


  /** Рендер тела ответа. */
  private def freeAdvMdrBody(banForm: Form[String], respStatus: Status)
                            (implicit request: RequestWithAd[_]): Future[Result] = {
    import request.mad
    implicit val ctx = implicitly[Context]
    // 2015.apr.20: Использован функционал выдачи для сбора данных по рендеру. По идее это ок, но лучше бы протестировать.
    val brArgsFut = scUtil.focusedBrArgsFor(mad)(ctx)
    val producerFut = mNodeCache.maybeGetByIdCached( n2NodesUtil.madProducerId(mad) )
      .map { _.get }
    for {
      _brArgs   <- brArgsFut
      _producer <- producerFut
    } yield {
      val args = FreeAdvMdrRArgs(
        brArgs    = _brArgs,
        producer  = _producer,
        banFormM  = banForm
      )
      val render = freeAdvMdrTpl(args)(ctx)
      respStatus(render)
    }
  }


  /** Сабмит одобрения пост-модерации бесплатного размещения.
    * Нужно выставить в карточку данные о модерации. */
  def freeAdvMdrAccept(adId: String) = IsSuperuserMad(adId).async { implicit request =>
    // Запускаем сохранение данных модерации.
    val updFut = _updMdrEdge {
      MEdgeInfo(
        flag   = Some(true),
        dateNi = _someNow
      )
    }

    // После завершения асинхронный операций, вернуть результат.
    for (_ <- updFut) yield {
      // Обновление выполнено. Пора отредиректить юзера на страницу модерации других карточек.
      val args = MdrSearchArgs(
        hideAdIdOpt = Some(adId)
      )
      Redirect( routes.SysMdr.freeAdvs(args) )
        .flashing(FLASH.SUCCESS -> "Карточка помечена как проверенная.")
    }
  }

  private def _someNow = Some( DateTime.now )

  /** Код обновления эджа модерации живёт здесь. */
  private def _updMdrEdge(info: MEdgeInfo)(implicit request: RequestWithAd[_]): Future[String] = {
    // Сгенерить обновлённые данные модерации.
    val mdr2 = MEdge(
      nodeId    = request.pwOpt.get.personId,
      predicate = MPredicates.ModeratedBy,
      info      = info
    )
    val mdr2map = MNodeEdges.edgesToMap(mdr2)

    // Запускаем сохранение данных модерации.
    MNode.tryUpdate(request.mad) { mad0 =>
      mad0.copy(
        edges = mad0.edges.copy(
          out = mad0.edges.out ++ mdr2map
        )
      )
    }
  }

  /** Сабмит формы блокирования бесплатного размещения рекламной карточки. */
  def freeAdvMdrBan(adId: String) = IsSuperuserMad(adId).async { implicit request =>
    banFreeAdvFormM.bindFromRequest().fold(
      {formWithErrors =>
        debug(s"freeAdvMdrBan($adId): Failed to bind form:\n${formatFormErrors(formWithErrors)}")
        freeAdvMdrBody(formWithErrors, NotAcceptable)
      },
      {reason =>
        // Сохранить отказ в модерации.
        val saveFut = _updMdrEdge {
          MEdgeInfo(
            dateNi    = _someNow,
            commentNi = Some(reason),
            flag      = Some(false)
          )
        }

        for (_ <- saveFut) yield {
          val args = MdrSearchArgs(
            hideAdIdOpt = Some(adId)
          )
          Redirect( routes.SysMdr.freeAdvs(args) )
            .flashing(FLASH.ERROR -> "Карточка убрана из бесплатной выдачи.")
        }
      }
    )
  }

}

