package controllers

import com.google.inject.Inject
import io.suggest.common.fut.FutureUtil
import io.suggest.di.IEsClient
import io.suggest.event.SioNotifierStaticClientI
import io.suggest.model.n2.extra.mdr.MFreeAdv
import models.mdr._
import models.msys.MSysMdrFreeAdvsTplArgs
import org.elasticsearch.client.Client
import play.api.i18n.MessagesApi
import play.api.mvc.Result
import util.PlayMacroLogsImpl
import util.acl.{RequestWithAd, IsSuperuserMad, IsSuperuser}
import util.lk.LkAdUtil
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
    var madsFut = MAd.findSelfAdvNonMdr(args)
    for (hai <- args.hideAdIdOpt) {
      madsFut = madsFut.map { mads0 =>
        mads0.filter(_.id.get != hai)
      }
    }
    val producersFut = madsFut flatMap { mads =>
      val producerIds = mads.map(_.producerId).toSet ++ args.producerId.toSet
      mNodeCache.multiGet( producerIds )
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
    val producerFut = mNodeCache.getById(mad.producerId)
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
    // Сгенерить обновлённые данные модерации.
    val mdr2 = Some(MFreeAdv(
      isAllowed = true,
      byUser    = request.pwOpt.get.personId
    ))

    // Запускаем сохранение данных модерации.
    val updFut = MAd.tryUpdate(request.mad) { mad0 =>
      mad0.copy(
        moderation = mad0.moderation.copy(
          freeAdv = mdr2
        )
      )
    }

    // После завершения асинхронный операций, вернуть результат.
    for(_ <- updFut) yield {
      // Обновление выполнено. Пора отредиректить юзера на страницу модерации других карточек.
      val args = MdrSearchArgs(
        hideAdIdOpt = Some(adId)
      )
      Redirect( routes.SysMdr.freeAdvs(args) )
        .flashing(FLASH.SUCCESS -> "Карточка помечена как проверенная.")
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
        val freeAdvOpt2 = Some(MFreeAdv(
          isAllowed = false,
          byUser    = request.pwOpt.get.personId,
          reason    = Some(reason)
        ))

        val saveFut = MAd.tryUpdate(request.mad) { mad0 =>
          mad0.copy(
            moderation = mad0.moderation.copy(
              freeAdv = freeAdvOpt2
            )
          )
        }

        for(_ <- saveFut) yield {
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

