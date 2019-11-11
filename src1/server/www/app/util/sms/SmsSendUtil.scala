package util.sms

import io.suggest.util.logs.MacroLogsImpl
import javax.inject.{Inject, Singleton}
import models.sms.{ISmsSendResult, MSmsSend}
import play.api.Configuration
import play.api.inject.Injector
import util.sms.smsru.SmsRuClient

import scala.concurrent.{ExecutionContext, Future}
import scala.reflect.ClassTag
import scala.util.Try
import io.suggest.common.empty.OptionUtil.BoolOptOps
import japgolly.univeq._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 31.05.19 15:08
  * Description: Фасад для отправки смс.
  */
@Singleton
class SmsSendUtil @Inject()(
                             injector                   : Injector,
                             implicit private val ec    : ExecutionContext,
                           )
  extends MacroLogsImpl
{

  /** Список классов смс-клиентов для разных сервисов. */
  lazy val SMS_CLIENT_CLASSES = {
    List[ClassTag[_ <: ISmsSendClient]](
      ClassTag( classOf[DummySmsSendClient] ),
      ClassTag( classOf[SmsRuClient] )
    )
  }

  /** Активен ли тестовый режим в конфиге? */
  lazy val TEST_MODE = injector
    .instanceOf[Configuration]
    .getOptional[Boolean]( "sms.test" )
    .getOrElseFalse


  private def _firstAvailClient(): Future[ISmsSendClient] = {
    _firstAvailClient( SMS_CLIENT_CLASSES )
  }
  private def _firstAvailClient(rest: List[ClassTag[_ <: ISmsSendClient]]): Future[ISmsSendClient] = {
    rest match {
      case hd :: tl =>
        lazy val logPrefix = s"_firstAvailClient($hd):"
        // Попробовать инжекцию:
        val fut0 = for {
          smsClient <- Try( injector.instanceOf(hd) )
            .fold( Future.failed, Future.successful )
          isReady <- smsClient.isReady()
          if isReady
        } yield {
          LOGGER.trace(s"$logPrefix Sms-client ready: $smsClient")
          smsClient
        }
        // Текущий клиент не работает, перейти к следующему клиенту:
        fut0.recoverWith { case ex =>
          def logMsg = s"$logPrefix Client $hd not ready or failed"
          if (ex.isInstanceOf[NoSuchElementException]) LOGGER.trace( logMsg )
          else LOGGER.warn( logMsg, ex )
          _firstAvailClient( tl )
        }

      case Nil =>
        throw new IllegalStateException("No ready sms-clients available")
    }
  }


  /** Допиливание смски перед отправкой.
    *
    * @param sms Исходная отправляемая смс.
    * @return
    */
  private def _prepareSmsForSend(sms: MSmsSend): MSmsSend = {
    var updatesAcc = List.empty[MSmsSend => MSmsSend]

    if (MSmsSend.isTest.get(sms) !=* TEST_MODE)
      updatesAcc ::= MSmsSend.isTest.set( TEST_MODE )

    if (updatesAcc.isEmpty) sms
    else updatesAcc.reduce(_ andThen _)(sms)
  }


  /** Отправка смс.
    *
    * @param sms Отправляемая смс.
    * @return Фьючерс с результатом отправки.
    */
  def smsSend(sms: MSmsSend): Future[Seq[ISmsSendResult]] = {
    val firstAvailClientFut = _firstAvailClient()
    val sms2 = _prepareSmsForSend( sms )
    LOGGER.trace(s"smsSend(): test?${sms.isTest} translit?${sms.translit}${sms.from.fold("")(" from=" + _)}${sms.ttl.fold("")(" ttl=" + _)}${sms.timeAt.fold("")(" timeAt=" + _)}\n ${sms.msgs.view.mapValues(_.mkString(" | ")).mkString(",\n ")}")
    for {
      smsClient   <- firstAvailClientFut
      res         <- smsClient.smsSend( sms2 )
    } yield {
      res
    }
  }

}
