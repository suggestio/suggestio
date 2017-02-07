package controllers.sc

import controllers.SioController
import io.suggest.stat.m.{MAction, MActionTypes, MComponents, MDiag}
import io.suggest.util.logs.MacroLogsImpl
import models.mctx.Context
import models.msc.MScRemoteDiag
import play.api.data.Forms._
import play.api.data._
import play.api.http.MimeTypes
import util.FormUtil._
import util.acl.{BruteForceProtect, MaybeAuth}
import util.di.IScStatUtil
import util.geo.IGeoIpUtilDi

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 14.10.14 18:54
 * Description: Сборка js-ошибок с клиентов и сохранение оных в модель.
 * Клиенты могут слать всякую хрень.
 */
trait ScRemoteError
  extends SioController
  with MacroLogsImpl
  with BruteForceProtect
  with MaybeAuth
  with IGeoIpUtilDi
  with IScStatUtil
{

  import mCommonDi._

  /** Малый дедлайн, т.к. новая выдача бывает любит пофлудить ошибками. */
  override def BRUTEFORCE_TRY_COUNT_DEADLINE_DFLT = 3

  /** Маппинг для вычитывания результата из тела POST. */
  private def errorFormM: Form[MScRemoteDiag] = {
    import io.suggest.err.ErrorConstants.Remote._
    Form(
      mapping(
        MSG_FN -> {
          nonEmptyText(minLength = 1, maxLength = 8192)
            .transform[String](strTrimF, strIdentityF)
        },
        URL_FN -> {
          optional( text(minLength = 8, maxLength = 1024) )
            .transform[Option[String]](emptyStrOptToNone, identity)
        },
        STATE_FN -> {
          optional(text(maxLength = 2048))
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
  def handleScError = MaybeAuth(U.PersonNode).async { implicit request =>
    bruteForceProtected {
      lazy val logPrefix = s"handleScError(${System.currentTimeMillis()}) [${request.remoteAddress}]:"
      errorFormM.bindFromRequest().fold(
        {formWithErrors =>
          LOGGER.warn(logPrefix + " Request body bind failed:\n " + formatFormErrors(formWithErrors))
          NotAcceptable("Failed to parse response. See server logs.")
        },
        {merr0 =>
          val remoteAddrFixed = geoIpUtil.fixRemoteAddr( request.remoteAddress )

          // Запустить геолокацию текущего юзера по IP.
          val geoLocOptFut = geoIpUtil.findIpCached( remoteAddrFixed.remoteAddr )
          // Запустить получение инфы о юзере. Без https тут всегда None.
          val userSaOptFut = scStatUtil.userSaOptFutFromRequest()
          val _ctx = implicitly[Context]

          for {
            _geoLocOpt <- geoLocOptFut
            _userSaOpt <- userSaOptFut

            stat2 = new scStatUtil.Stat2 {
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
              override def scComponents = MComponents.Error :: super.scComponents
              override def userSaOpt = _userSaOpt
              override def ctx = _ctx
              override def geoIpLoc = _geoLocOpt
            }

            // Сохраняем в базу отчёт об ошибке.
            merrId <- scStatUtil.saveStat(stat2)
          } yield {
            LOGGER.trace(logPrefix + s" Saved remote error as stat[$merrId]." )
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
