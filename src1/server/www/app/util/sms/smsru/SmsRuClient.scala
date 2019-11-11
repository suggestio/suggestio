package util.sms.smsru

import io.suggest.text.Validators
import io.suggest.util.logs.MacroLogsImpl
import javax.inject.{Inject, Singleton}
import models.sms.smsRu.{MSmsRuResult, MSmsRuSendQs}
import models.sms.{ISmsSendResult, MSmsSend}
import play.api.{ConfigLoader, Configuration}
import play.api.inject.Injector
import play.api.libs.ws.WSClient
import play.api.mvc.QueryStringBindable
import util.sms.ISmsSendClient

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}
import japgolly.univeq._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 30.05.19 18:00
  * Description: Реализация клиента отправки сообщений для sms.ru.
  */
@Singleton
final class SmsRuClient @Inject() (
                                    injector                  : Injector,
                                    wsClient                  : WSClient,
                                    implicit private val ec   : ExecutionContext,
                                  )
  extends ISmsSendClient
  with MacroLogsImpl
{

  private def CONF_PREFIX = "sms.smsru"

  /** id зарегистрированного приложения на sms.ru. */
  private lazy val (appIdOpt, fromDflt): (Option[String], Option[String]) = {
    val confOpt       = injector.instanceOf[Configuration].getOptional[Configuration]( CONF_PREFIX )
    def __get[A: ConfigLoader](subPath: String): Option[A] =
      confOpt.flatMap( _.getOptional[A](subPath) )
    val appIdOpt      = __get[String]("app.id")
    val fromDfltOpt   = __get[String]("from")
    (appIdOpt, fromDfltOpt)
  }

  /** Ссылка для запроса отправки смс. */
  def SEND_URL_PREFIX = "https://sms.ru/sms/send?"

  /** sms.ru требует слать не более 100 смс за один запрос. */
  def MAX_SMS_PER_REQUEST = 100


  override def isReady(): Future[Boolean] =
    Future.successful( appIdOpt.nonEmpty )


  /** Отправить смс на указанный номер (номера). */
  override def smsSend(sms: MSmsSend): Future[Seq[ISmsSendResult]] = {
    val apiId = appIdOpt.get

    lazy val logPrefix = s"smsSend()#${System.currentTimeMillis()}:"

    // key выставляем в null, чтобы явно была ошибка при обращении к ключу с потолка.
    val sms2 = MSmsRuSendQs.from( sms, apiId, fromDflt )
    val url = SEND_URL_PREFIX + implicitly[QueryStringBindable[MSmsRuSendQs]].unbind( null, sms2 )
    LOGGER.debug(s"$logPrefix Sending ${sms.msgs.size} sms-messages to numbers: ${sms.msgs.iterator.map(_._1).mkString(", ")}\n POST $url")

    val wsClient = injector.instanceOf[WSClient]

    // Подготовить данные для реквестов, разбив поток по максимум 100 штук.
    Future.traverse {
      (for {
        (phone, messages) <- sms.msgs.iterator
        if messages.nonEmpty
        phoneNorm = Validators.normalizePhoneNumber( phone )
        oneMsg <- messages
      } yield {
        val phoneKey = "to[" + phoneNorm + "]"
        phoneKey -> oneMsg
      })
        .sliding(MAX_SMS_PER_REQUEST, MAX_SMS_PER_REQUEST)
        .map { smsForReq =>
          smsForReq
            .groupBy(_._1)
            .view
            .mapValues(_.map(_._2))
            .toMap
        }
        .toSeq
    } { formData =>
      // Запустить запрос к sms.ru
      val reqId = System.currentTimeMillis()
      LOGGER.trace(s"$logPrefix Starting request#$reqId for ${formData.size} numbers for ${formData.valuesIterator.flatten.size} sms.")
      for {
        wsResult <- wsClient
          .url( url )
          .withRequestTimeout( 10.seconds )
          .post( formData )
      } yield {
        LOGGER.debug(s"$logPrefix Done req#$reqId status=${wsResult.status} ${wsResult.statusText}")
        if (wsResult.status !=* 200)
          LOGGER.trace(s" ${wsResult.body}")
        wsResult
          .json
          .validate[MSmsRuResult]
          // Не распарсилось. Не ясно, отправлено ли хоть что-нибудь. Считаем это ошибкой в софте.
          .recoverTotal { error =>
            throw new RuntimeException( s"$logPrefix Failed to parse resp JSON:\n src = ${wsResult.body}\n $error" )
          }
      }
    }
  }

}
