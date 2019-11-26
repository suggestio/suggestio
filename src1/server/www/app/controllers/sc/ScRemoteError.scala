package controllers.sc

import io.suggest.stat.m.{MAction, MActionTypes, MComponents, MDiag, MStats}
import io.suggest.util.logs.MacroLogsImpl
import models.mctx.Context
import models.msc.MScRemoteDiag
import play.api.data.Forms._
import play.api.data._
import play.api.http.MimeTypes
import util.FormUtil._
import util.acl.{IBruteForceProtect, IMaybeAuth}
import util.geo.IGeoIpUtilDi
import util.stat.IStatUtil

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 14.10.14 18:54
 * Description: Сборка js-ошибок с клиентов и сохранение оных в модель.
 * Клиенты могут слать всякую хрень.
 */
trait ScRemoteError
  extends ScController
  with MacroLogsImpl
  with IBruteForceProtect
  with IMaybeAuth
  with IGeoIpUtilDi
  with IStatUtil
{

  import sioControllerApi._
  import mCommonDi._

  //private val _BFP_ARGS = bruteForceProtect.ARGS_DFLT.withTryCountDeadline(1)

  private def SAVE_TO_MSTAT = false

  private lazy val mstats = current.injector.instanceOf[MStats]


  /** Маппинг для вычитывания результата из тела POST. */
  private val errorFormM: Form[MScRemoteDiag] = {
    import io.suggest.err.ErrorConstants.Remote._
    Form(
      mapping(
        MSG_FN -> {
          nonEmptyText(minLength = MSG_LEN_MIN, maxLength = MSG_LEN_MAX)
            .transform[String](strTrimF, strIdentityF)
        },
        URL_FN -> {
          optional( text(minLength = URL_LEN_MIN, maxLength = URL_LEN_MAX) )
            .transform[Option[String]](emptyStrOptToNone, identity)
        },
        STATE_FN -> {
          optional(text(maxLength = STATE_LEN_MAX))
            .transform[Option[String]](emptyStrOptToNone, identity)
        }
      )
      {(msg, urlOpt, state) =>
        MScRemoteDiag(
          message = msg,
          url     = urlOpt,
          state   = state
        )
      }
      {merr =>
        Some((merr.message, merr.url, merr.state))
      }
    )
  }


  /**
   * Реакция на ошибку в showcase (в выдаче). Если слишком много запросов с одного ip, то экшен начнёт тупить.
   * @return NoContent или NotAcceptable.
   */
  def handleScError = /*bruteForceProtect(_BFP_ARGS)*/ {
    maybeAuth(U.PersonNode).async { implicit request =>
      lazy val logPrefix = s"handleScError(${System.currentTimeMillis()}) [${request.remoteClientAddress}]:"
      errorFormM.bindFromRequest().fold(
        {formWithErrors =>
          LOGGER.warn(logPrefix + " Request body bind failed:\n " + formatFormErrors(formWithErrors))
          NotAcceptable("Failed to parse response. See server logs.")
        },
        {merr0 =>
          val remoteAddrFixed = geoIpUtil.fixRemoteAddr( request.remoteClientAddress )

          // Запустить геолокацию текущего юзера по IP.
          val geoLocOptFut = geoIpUtil.findIpCached( remoteAddrFixed.remoteAddr )
          // Запустить получение инфы о юзере. Без https тут всегда None.
          val userSaOptFut = statUtil.userSaOptFutFromRequest()
          val _ctx = implicitly[Context]

          val stat2Fut = for {
            _geoLocOpt <- geoLocOptFut
            _userSaOpt <- userSaOptFut
          } yield {
            new statUtil.Stat2 {
              override def logMsg = Some("Sc-remote-error")

              override def uri: Option[String] = {
                merr0.url.orElse( super.uri )
              }
              override def diag: MDiag = {
                MDiag(
                  message = Option(merr0.message),
                  state   = merr0.state
                )
              }
              override def statActions: List[MAction] = {
                val maction = MAction(
                  actions   = Seq( MActionTypes.ScIndexCovering ),
                  nodeId    = Nil,
                  nodeName  = Nil
                )
                maction :: Nil
              }
              override def components = MComponents.Error :: super.components
              override def userSaOpt = _userSaOpt
              override def ctx = _ctx
              override def geoIpLoc = _geoLocOpt
            }
          }

          // Куда сохранять? В логи или просто на сервере в логи отрендерить?
          for {
            stat2 <- stat2Fut
            _ <- statUtil.maybeSaveGarbageStat( stat2 )
          } yield {
            NoContent
              // Почему-то по дефолту приходит text/html, и firefox dev 51 пытается распарсить ответ, и выкидывает в логах
              // ошибку, что нет root тега в ответе /sc/error.
              .as( MimeTypes.TEXT )
          }
        }
      )
    }
  }

}
