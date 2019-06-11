package util.sms

import akka.actor.ActorSystem
import javax.inject.Inject
import models.sms.{ISmsSendResult, MSmsSend, MSmsSendResult, MSmsSendStatus}
import play.api.Configuration
import io.suggest.common.empty.OptionUtil.BoolOptOps
import io.suggest.util.logs.MacroLogsImpl

import scala.concurrent.{ExecutionContext, Future, Promise}
import scala.concurrent.duration._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 06.06.19 15:37
  * Description: Заглушка вместо клиента отправки смс-сообщений.
  * Имитирует успешную отправку.
  */
class DummySmsSendClient @Inject()(
                                    configuration             : Configuration,
                                    actorSystem               : ActorSystem,
                                    implicit private val ec   : ExecutionContext,
                                  )
  extends ISmsSendClient
  with MacroLogsImpl
{

  /** Активен ли клиент? */
  val IS_AVAILABLE = configuration
    .getOptional[Boolean]( "sms.dummy.enabled" )
    .getOrElseFalse


  override def isReady(): Future[Boolean] =
    Future.successful( IS_AVAILABLE )

  override def smsSend(sms: MSmsSend): Future[Seq[ISmsSendResult]] = {
    val p = Promise[Seq[ISmsSendResult]]()

    actorSystem.scheduler.scheduleOnce( 500 milliseconds ) {
      LOGGER.info(s"smsSend(): FAKE sent sms to\n ${sms.msgs.mapValues(_.mkString(" | ")).mkString("\n ")}")
      val r = MSmsSendResult(
        isOk        = true,
        statusText  = None,
        smsInfo     = sms.msgs.mapValues { _ =>
          MSmsSendStatus(
            isOk = true,
          )
        }
      )
      p.success( r :: Nil )
    }

    p.future
  }

}
