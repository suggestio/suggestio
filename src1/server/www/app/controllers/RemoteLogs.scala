package controllers

import io.suggest.log.MLogReport
import io.suggest.stat.m.{MAction, MActionTypes, MComponents, MDiag}
import io.suggest.util.logs.MacroLogsImpl

import javax.inject.Inject
import models.mctx.Context
import play.api.http.MimeTypes
import play.api.mvc.BodyParser
import util.acl.{MaybeAuth, SioControllerApi}
import util.geo.GeoIpUtil
import util.stat.StatUtil
import io.suggest.scalaz.ScalazUtil.Implicits._
import play.api.inject.Injector

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 30.04.2020 21:00
  * Description: Контроллер сбора логов с клиентских устройств.
  */
final class RemoteLogs @Inject() (
                                   injector                   : Injector,
                                   sioControllerApi           : SioControllerApi,
                                 )
  extends MacroLogsImpl
{

  private lazy val maybeAuth = injector.instanceOf[MaybeAuth]
  private lazy val statUtil = injector.instanceOf[StatUtil]
  private lazy val geoIpUtil = injector.instanceOf[GeoIpUtil]

  /** To be tolerant to validation errors. */
  private def ALLOW_INVALID = true

  import sioControllerApi._

  private def _logReceiverBP: BodyParser[MLogReport] = {
    parse
      .json[MLogReport]
      .validate { logRep =>
        MLogReport.validate( logRep ).fold(
          {fails =>
            if (ALLOW_INVALID) {
              LOGGER.warn(s"_logReceiverBP: Failures during validation:\n ${fails.iterator.mkString("\n ")}")
              Right( logRep )
            } else {
              Left( NotAcceptable( s"Validation failed:\n ${fails.iterator.mkString("\n ")}" ) )
            }
          },
          Right.apply
        )
      }
  }

  /** POST получение логов в обновлённом JSON-формате.
    *
    * @return 204 No Content.
    */
  def receive() = {
    maybeAuth( U.PersonNode ).async( _logReceiverBP ) { implicit request =>
      //lazy val logPrefix = s"receive(${System.currentTimeMillis()}) [${request.remoteClientAddress}]:"

      // Запустить геолокацию текущего юзера по IP.
      val geoLocOptFut = geoIpUtil.findIpCached( request.remoteClientAddress )
      // Запустить получение инфы о юзере. Без https тут всегда None.
      val userSaOptFut = statUtil.userSaOptFutFromRequest()
      implicit val _ctx = implicitly[Context]

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
        // text/plain: Почему-то по дефолту приходит text/html, и firefox dev 51 пытается распарсить ответ,
        // и выкидывает в логах ошибку, что нет root тега в ответе /sc/error.
        NoContent
          .as( MimeTypes.TEXT )
      }
    }
  }

}
