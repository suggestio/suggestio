package controllers

import io.suggest.log.MLogReport
import io.suggest.stat.m.{MAction, MActionTypes, MComponents, MDiag}
import io.suggest.util.logs.MacroLogsImpl

import javax.inject.Inject
import models.mctx.Context
import models.msc.MScRemoteDiag
import play.api.data.Form
import play.api.data.Forms.{mapping, nonEmptyText, optional, text}
import play.api.http.MimeTypes
import play.api.mvc.BodyParser
import util.FormUtil.{emptyStrOptToNone, strIdentityF, strTrimF}
import util.acl.{MaybeAuth, SioControllerApi}
import util.geo.GeoIpUtil
import util.stat.StatUtil
import io.suggest.scalaz.ScalazUtil.Implicits._
import util.cdn.CorsUtil

import scala.concurrent.ExecutionContext

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 30.04.2020 21:00
  * Description: Контроллер сбора логов с клиентских устройств.
  */
final class RemoteLogs @Inject() (
                                   statUtil                   : StatUtil,
                                   maybeAuth                  : MaybeAuth,
                                   geoIpUtil                  : GeoIpUtil,
                                   corsUtil                   : CorsUtil,
                                   sioControllerApi           : SioControllerApi,
                                   implicit private val ec    : ExecutionContext,
                                 )
  extends MacroLogsImpl
{

  import sioControllerApi._


  /** Маппинг для вычитывания результата из тела POST. */
  private def errorFormM: Form[MScRemoteDiag] = {
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
      )
      {(msg, urlOpt) =>
        MScRemoteDiag(
          message = msg,
          url     = urlOpt,
        )
      }
      {merr =>
        Some((merr.message, merr.url))
      }
    )
  }


  /**
    * Реакция на ошибку в showcase (в выдаче). Если слишком много запросов с одного ip, то экшен начнёт тупить.
    * TODO Не нужно с мая 2020, оставлено для старых версий выдачи на какое-то время.
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
                )
              }
              override def statActions: List[MAction] = {
                val maction = MAction(
                  actions   = MActionTypes.ScIndexCovering :: Nil,
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


  private def _logReceiverBP: BodyParser[MLogReport] = {
    parse
      .json[MLogReport]
      .validate { logRep =>
        MLogReport.validate( logRep ).fold(
          {fails =>
            Left( NotAcceptable( s"Validation failed:\n ${fails.iterator.mkString("\n ")}" ) )
          },
          Right.apply
        )
      }
  }

  /** POST получение логов в обновлённом JSON-формате.
    *
    * @return 204 No Content.
    */
  def receive = {
    maybeAuth( U.PersonNode ).async( _logReceiverBP ) { implicit request =>
      //lazy val logPrefix = s"receive(${System.currentTimeMillis()}) [${request.remoteClientAddress}]:"
      val remoteAddrFixed = geoIpUtil.fixRemoteAddr( request.remoteClientAddress )

      // Запустить геолокацию текущего юзера по IP.
      val geoLocOptFut = geoIpUtil.findIpCached( remoteAddrFixed.remoteAddr )
      // Запустить получение инфы о юзере. Без https тут всегда None.
      val userSaOptFut = statUtil.userSaOptFutFromRequest()
      val _ctx = implicitly[Context]

      val diagMsg = request.body.msgs.mkString("\n")

      // Куда сохранять? В логи или просто на сервере в логи отрендерить?
      for {
        _geoLocOpt <- geoLocOptFut
        _userSaOpt <- userSaOptFut
        stat2 = new statUtil.Stat2 {
          override def logMsg = Some("Sc-remote-error")
          override def diag: MDiag = {
            if (statUtil.SAVE_GARBAGE_TO_MSTAT) {
              MDiag(
                message = Some( diagMsg ),
              )
            } else {
              MDiag.empty
            }
          }
          override def statActions: List[MAction] = {
            val maction = MAction(
              actions   = MActionTypes.ScIndexCovering :: Nil,
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
        _ <- statUtil.maybeSaveGarbageStat(
          stat2,
          logTail = diagMsg,
        )
      } yield {
        corsUtil.withCorsIfNeeded(
          NoContent
            // Почему-то по дефолту приходит text/html, и firefox dev 51 пытается распарсить ответ, и выкидывает в логах
            // ошибку, что нет root тега в ответе /sc/error.
            .as( MimeTypes.TEXT )
        )
      }
    }
  }

}
