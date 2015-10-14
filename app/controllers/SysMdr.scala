package controllers

import com.google.inject.Inject
import io.suggest.di.IEsClient
import io.suggest.event.SioNotifierStaticClientI
import io.suggest.ym.model.ad.FreeAdvStatus
import models.mdr._
import org.elasticsearch.client.Client
import org.elasticsearch.index.engine.VersionConflictEngineException
import play.api.i18n.MessagesApi
import play.twirl.api.Html
import util.PlayMacroLogsImpl
import util.acl.{AbstractRequestWithPwOpt, IsSuperuser}
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
  override val messagesApi          : MessagesApi,
  override implicit val ec          : ExecutionContext,
  override implicit val esClient    : Client,
  override implicit val sn          : SioNotifierStaticClientI
)
  extends SioControllerImpl
  with PlayMacroLogsImpl
  with IEsClient
{

  import LOGGER._

  /** Отобразить начальную страницу раздела модерации рекламных карточек. */
  def index = IsSuperuser { implicit request =>
    Ok(mdrIndexTpl())
  }

  /** Страница с бесплатно-размещёнными рекламными карточками, подлежащими модерации s.io.
    * @param args Аргументы для поиска (QSB).
    * @param hideAdId Можно скрыть какую-нибудь карточку. Полезно скрывать отмодерированную, т.к. она
    *                 некоторое время ещё будет висеть на этой странице.
    */
  def freeAdvs(args: MdrSearchArgs, hideAdId: Option[String]) = IsSuperuser.async { implicit request =>
    var madsFut = MAd.findSelfAdvNonMdr(args)
    if (hideAdId.isDefined) {
      val hai = hideAdId.get
      madsFut = madsFut.map { mads0 =>
        mads0.filter(_.id.get != hai)
      }
    }
    val producersFut = madsFut flatMap { mads =>
      val producerIds = mads.map(_.producerId).toSet ++ args.producerId.toSet
      MAdnNodeCache.multiGet( producerIds )
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

    for {
      brArgss   <- brArgssFut
      prodsMap  <- prodsMapFut
    } yield {
      Ok(freeAdvsTpl(
        brArgss,
        prodsMap = prodsMap,
        currPage = args.page,
        hasNextPage = brArgss.size >= MdrSearchArgs.FREE_ADVS_PAGE_SZ,
        adnNodeOpt = args.producerId.flatMap(prodsMap.get)
      ))
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
  def freeAdvMdr(adId: String) = IsSuperuser.async { implicit request =>
    freeAdvMdrBody(adId, banFreeAdvFormM)
      .map { Ok(_) }
  }

  /** Рендер тела ответа. */
  private def freeAdvMdrBody(adId: String, banForm: Form[String])(implicit request: AbstractRequestWithPwOpt[_]): Future[Html] = {
    val madFut = MAd.getById(adId)
      .map(_.get)
    val ctx = implicitly[Context]
    madFut flatMap { mad =>
      // 2015.apr.20: Использован функционал выдачи для сбора данных по рендеру. По идее это ок, но лучше бы протестировать.
      val brArgsFut = ShowcaseUtil.focusedBrArgsFor(mad)(ctx)
      val producerFut = MAdnNodeCache.getById(mad.producerId)
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
        freeAdvMdrTpl(args)(ctx)
      }
    }
  }

  /** Сколько попыток обновить данные по модерации надо делать, прежде чем бросать это дело? */
  def UPDATE_MDR_TRY_MAX = 5

  /** Сабмит одобрения пост-модерации бесплатного размещения.
    * Нужно выставить в карточку данные о модерации. */
  def freeAdvMdrAccept(adId: String) = IsSuperuser.async { implicit request =>
    // Для атомарного обновления поля используем optimistic locking (ES versionising)
    def tryUpdate(counter: Int = 0): Future[_] = {
      MAd.getById(adId) flatMap { madOpt =>
        val mad = madOpt.get
        mad.moderation = mad.moderation.copy (
          freeAdv = Some(FreeAdvStatus(
            isAllowed = true,
            byUser = request.pwOpt.get.personId
          ))
        )
        mad.saveModeration
          .recoverWith {
            case ex: VersionConflictEngineException =>
              val tryMax = UPDATE_MDR_TRY_MAX
              if (counter < tryMax) {
                // Счетчик попыток ещё не истёк. Повторить попытку.
                val counter1 = counter + 1
                debug(s"freeAdvMdrAccept($adId): ES said: 409 Version conflict. Retrying ($counter1/$tryMax)...")
                tryUpdate(counter1)
              } else {
                Future failed new RuntimeException(s"Too many version conflicts: $counter, lastVsn = ${mad.versionOpt}", ex)
              }
          }
      }
    }
    tryUpdate() map { _ =>
      // Обновление выполнено. Пора отредиректить юзера на страницу модерации других карточек.
      Redirect(routes.SysMdr.freeAdvs(hideAdId = Some(adId)))
        .flashing("success" -> "Карточка помечена как проверенная.")
    }
  }

  /** Сабмит формы блокирования бесплатного размещения рекламной карточки. */
  def freeAdvMdrBan(adId: String) = IsSuperuser.async { implicit request =>
    banFreeAdvFormM.bindFromRequest().fold(
      {formWithErrors =>
        debug(s"freeAdvMdrBan($adId): Failed to bind form:\n${formatFormErrors(formWithErrors)}")
        freeAdvMdrBody(adId, formWithErrors)
          .map { NotAcceptable(_) }
      },
      {reason =>
        // Отклонено. Надо бы снять карточку из бесплатного размещения и выставить причину в результат модерации.
        def tryUpdate(counter: Int = 0): Future[_] = {
          MAd.getById(adId) flatMap { madOpt =>
            val mad = madOpt.get
            mad.moderation = mad.moderation.copy(
              freeAdv = Some(FreeAdvStatus(
                isAllowed = false,
                byUser = request.pwOpt.get.personId,
                reason = Some(reason)
              ))
            )
            mad.receivers = mad.receivers.filter {
              case (rcvrId, _)  =>  rcvrId != mad.producerId
            }
            mad.save.recoverWith {
              case ex: VersionConflictEngineException =>
                val tryMax = UPDATE_MDR_TRY_MAX
                if (counter < tryMax) {
                  val counter1 = counter + 1
                  debug(s"freeAdvMdrBan($adId): ES said: 409 Vsn conflict. Retrying ($counter1/$tryMax)...")
                  tryUpdate(counter1)
                } else {
                  Future failed new RuntimeException(s"Too many version conflicts: $counter, lastVsn = ${mad.versionOpt}", ex)
                }
            }
          }
        }
        tryUpdate() map { _ =>
          Redirect(routes.SysMdr.freeAdvs(hideAdId = Some(adId)))
            .flashing("error" -> "Карточка убрана из бесплатной выдачи.")
        }
      }
    )
  }

}

