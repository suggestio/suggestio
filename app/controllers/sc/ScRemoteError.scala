package controllers.sc

import controllers.SioController
import models.GeoIp
import models.merr.{IMRemoteErrors, MRemoteError, MRemoteErrorTypes}
import play.api.data.Forms._
import play.api.data._
import util.FormUtil._
import util.PlayMacroLogsImpl
import util.acl.{BruteForceProtectCtl, MaybeAuth}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 14.10.14 18:54
 * Description: Сборка js-ошибок с клиентов и сохранение оных в модель.
 * Клиенты могут слать всякую хрень.
 */
trait ScRemoteError
  extends SioController
  with PlayMacroLogsImpl
  with BruteForceProtectCtl
  with MaybeAuth
  with IMRemoteErrors
{

  import mCommonDi._

  override def BRUTEFORCE_TRY_COUNT_DEADLINE_DFLT = 10

  /** Маппинг для вычитывания результата из тела POST. */
  private def errorFormM: Form[MRemoteError] = {
    import io.suggest.err.ErrorConstants.Remote._
    Form(
      mapping(
        MSG_FN -> {
          nonEmptyText(minLength = 1, maxLength = 1024)
            .transform[String](strTrimF, strIdentityF)
        },
        URL_FN -> {
          optional( text(minLength = 8, maxLength = 512) )
            .transform[Option[String]](emptyStrOptToNone, identity)
        },
        STATE_FN -> {
          optional(text(maxLength = 1024))
            .transform[Option[String]](emptyStrOptToNone, identity)
        }
      )
      {(msg, urlOpt, state) =>
        MRemoteError(
          errorType   = MRemoteErrorTypes.Showcase,
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
  def handleScError = MaybeAuth().async { implicit request =>
    bruteForceProtected {
      lazy val logPrefix = s"handleScError(${System.currentTimeMillis()}) [${request.remoteAddress}]:"
      errorFormM.bindFromRequest().fold(
        {formWithErrors =>
          LOGGER.debug(logPrefix + " Request body bind failed:\n " + formatFormErrors(formWithErrors))
          NotAcceptable("Failed to parse response. See server logs.")
        },
        {merr0 =>
          // Запустить геолокацию текущего юзера по IP.
          val geoLocOptFut = GeoIp
            .geoSearchInfoOpt
            .recover {
              // Should never happen. Подавляем возможную ошибку получения геоданных запроса.
              case ex: Exception =>
                LOGGER.warn("Suppressing exception for gsiOpt", ex)
                None
            }

          for {
            gsiOpt <- geoLocOptFut
            // Апдейтим забинденный формой отчёт об ошибке данными от сервера.
            merr1 = merr0.copy(
              ua          = request.headers.get(USER_AGENT).map(strTrimF),
              clientAddr  = request.remoteAddress,
              clIpGeo     = gsiOpt.map(_.geoPoint),
              clTown      = gsiOpt.flatMap(_.cityName),
              country     = gsiOpt.flatMap(_.countryIso2),
              isLocalCl   = gsiOpt.map(_.isLocalClient)
            )
            // Сохраняем в базу отчёт об ошибке.
            merrId <- mRemoteErrors.save(merr1)
          } yield {
            LOGGER.trace(logPrefix + " Saved remote error: " + merr1.copy(id = Some(merrId)) )
            NoContent
          }
        }
      )
    }
  }

}
