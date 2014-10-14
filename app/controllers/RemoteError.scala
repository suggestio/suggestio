package controllers

import models.MRemoteError
import play.api.data._, Forms._
import play.api.mvc.Action
import util.PlayMacroLogsImpl
import util.FormUtil._
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import util.event.SiowebNotifier.Implicts.sn
import util.SiowebEsUtil.client

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 14.10.14 18:54
 * Description: Сборка js-ошибок с клиентов и сохранение оных в модель.
 * Клиенты могут слать всякую хрень.
 */
object RemoteError extends SioController with PlayMacroLogsImpl with BruteForceProtect {

  import LOGGER._

  def errorFormM: Form[MRemoteError] = {
    Form(
      mapping(
        "msg" -> nonEmptyText(minLength = 1, maxLength = 1024)
          .transform[String](strTrimF, strIdentityF),
        "url" -> nonEmptyText(minLength = 8, maxLength = 512)
          .transform[String](strTrimF, strIdentityF)
      )
      {(msg, url) =>
        MRemoteError(
          msg = msg,
          url = url,
          clientAddr = "",
          ua = ""
        )
      }
      {merr =>
        Some((merr.msg, merr.url))
      }
    )
  }

  /**
   * Реакция на ошибку в showcase (в выдаче).
   * @return NoContent или NotAcceptable.
   */
  def handleShowcaseError = Action.async { implicit request =>
    errorFormM.bindFromRequest().fold(
      {formWithErrors =>
        debug("handleError(): Request data bind failed:\n " + formatFormErrors(formWithErrors))
        NotAcceptable("Failed to parse response. See server logs.")
      },
      {merr0 =>
        val merr1 = merr0.copy(
          ua = request.headers.get(USER_AGENT).get,
          clientAddr = request.remoteAddress
        )
        merr1.save.map { merrId =>
          NoContent
        }
      }
    )
  }

}
