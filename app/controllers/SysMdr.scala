package controllers

import io.suggest.ym.model.ad.FreeAdvStatus
import org.elasticsearch.index.engine.VersionConflictEngineException
import play.api.mvc.Result
import play.api.templates.HtmlFormat
import util.PlayMacroLogsImpl
import util.acl.{AbstractRequestWithPwOpt, IsSuperuser}
import scala.concurrent.ExecutionContext.Implicits.global
import util.SiowebEsUtil.client
import util.event.SiowebNotifier.Implicts.sn
import views.html.sys1.mdr._
import play.api.Play.{current, configuration}
import models._
import play.api.data.Form

import scala.concurrent.Future

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 19.06.14 10:45
 * Description: Sys Moderation - контроллер, заправляющий s.io-модерацией рекламных карточек.
 */
object SysMdr extends SioController with PlayMacroLogsImpl {

  import LOGGER._

  /** Отобразить начальную страницу раздела модерации рекламных карточек. */
  def index = IsSuperuser { implicit request =>
    Ok(mdrIndexTpl())
  }

  /** Страница с бесплатно-размещёнными рекламными карточками, подлежащими модерации s.io.
    * @param page номер страницы.
    * @param hideAdId Можно скрыть какую-нибудь карточку. Полезно скрывать отмодерированную, т.к. она
    *                 некоторое время ещё будет висеть на этой странице.
    */
  def freeAdvs(args: MdrSearchArgs, hideAdId: Option[String]) = IsSuperuser.async { implicit request =>
    MAd.findSelfAdvNonMdr(args) flatMap { mads0 =>
      val mads = hideAdId.fold(mads0) { hai => mads0.filter(_.id.get != hai) }
      MAdnNodeCache.multigetByIdCached( mads.map(_.producerId) ) map { producers =>
        val prodsMap = producers.map { p => p.id.get -> p }.toMap
        Ok(freeAdvsTpl(mads, prodsMap, args.page, hasNextPage = mads.size >= MdrSearchArgs.FREE_ADVS_PAGE_SZ))
      }
    }
  }


  val banFreeAdvFormM = {
    import play.api.data._, Forms._
    import util.FormUtil._
    Form(
      "reason" -> nonEmptyText(minLength = 4, maxLength = 1024)
        .transform(strTrimSanitizeF, strIdentityF)
    )
  }

  /** Страница для модерации одной карточки. */
  def freeAdvMdr(adId: String) = IsSuperuser.async { implicit request =>
    freeAdvMdrBody(adId, banFreeAdvFormM) map {
      Ok(_)
    }
  }

  /** Рендер тела ответа. */
  private def freeAdvMdrBody(adId: String, banForm: Form[String])(implicit request: AbstractRequestWithPwOpt[_]): Future[HtmlFormat.Appendable] = {
    MAd.getById(adId) flatMap { madOpt =>
      val mad = madOpt.get
      MAdnNodeCache.getByIdCached(mad.producerId) map { producerOpt =>
        val producer = producerOpt.get
        freeAdvMdrTpl(mad, producer, banForm)
      }
    }
  }

  /** Сколько попыток обновить данные по модерации надо делать, прежде чем бросать это дело? */
  val UPDATE_MDR_TRY_MAX = 5

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
              if (counter < UPDATE_MDR_TRY_MAX) {
                // Счетчик попыток ещё не истёк. Повторить попытку.
                val counter1 = counter + 1
                debug(s"freeAdvMdrAccept($adId): ES said: 409 Version conflict. Retrying ($counter1/$UPDATE_MDR_TRY_MAX)...")
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
          .map { Ok(_) }
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
                if (counter < UPDATE_MDR_TRY_MAX) {
                  val counter1 = counter + 1
                  debug(s"freeAdvMdrBan($adId): ES said: 409 Vsn conflict. Retrying ($counter1/$UPDATE_MDR_TRY_MAX)...")
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

