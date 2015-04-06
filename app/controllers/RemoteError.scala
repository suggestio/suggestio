package controllers

import models.{RemoteErrorTypes, GeoIp, MRemoteError}
import play.api.data._, Forms._
import play.api.i18n.MessagesApi
import util.PlayMacroLogsImpl
import util.FormUtil._
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import util.acl.{BruteForceProtectCtl, SioAction}
import util.SiowebEsUtil.client

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 14.10.14 18:54
 * Description: Сборка js-ошибок с клиентов и сохранение оных в модель.
 * Клиенты могут слать всякую хрень.
 */
class RemoteError(val messagesApi: MessagesApi) extends SioController with PlayMacroLogsImpl with BruteForceProtectCtl {

  import LOGGER._

  override def BRUTEFORCE_TRY_COUNT_DEADLINE_DFLT: Int = 10

  /** Маппинг для вычитывания результата из тела POST. */
  private def errorFormM: Form[MRemoteError] = {
    Form(
      mapping(
        "msg" -> {
          nonEmptyText(minLength = 1, maxLength = 1024)
            .transform[String](strTrimF, strIdentityF)
        },
        "url" -> {
          optional( text(minLength = 8, maxLength = 512) )
            .transform[Option[String]](emptyStrOptToNone, identity)
        },
        "state" -> {
          optional(text(maxLength = 1024))
            .transform[Option[String]](emptyStrOptToNone, identity)
        }
      )
      {(msg, urlOpt, state) =>
        MRemoteError(
          errorType   = RemoteErrorTypes.Showcase,
          msg         = msg,
          url         = urlOpt,
          state       = state,
          clientAddr  = ""
        )
      }
      {merr =>
        Some((merr.msg, merr.url, merr.state))
      }
    )
  }

  /**
   * Реакция на ошибку в showcase (в выдаче). Если слишком много запросов с одного ip, то экшен начнёт тупить.
   * @return NoContent или NotAcceptable.
   */
  def handleShowcaseError = SioAction.async { implicit request =>
    bruteForceProtected {
      errorFormM.bindFromRequest().fold(
        {formWithErrors =>
          debug("handleError(): Request body bind failed:\n " + formatFormErrors(formWithErrors))
          NotAcceptable("Failed to parse response. See server logs.")
        },
        {merr0 =>
          GeoIp.geoSearchInfoOpt
            .recover {
              // Should never happen. Подавляем возможную ошибку получения геоданных запроса.
              case ex: Exception =>
                warn("Suppressing exception for gsiOpt", ex)
                None
            }
            .flatMap { gsiOpt =>
              // Сохраняем в базу отчёт об ошибке.
              val merr1 = merr0.copy(
                ua          = request.headers.get(USER_AGENT).map(strTrimF),
                clientAddr  = request.remoteAddress,
                clIpGeo     = gsiOpt.map(_.geoPoint),
                clTown      = gsiOpt.flatMap(_.cityName),
                country     = gsiOpt.flatMap(_.countryIso2),
                isLocalCl   = gsiOpt.map(_.isLocalClient)
              )
              merr1.save.map { merrId =>
                NoContent
              }
            }
        }
      )
    }
  }

}
