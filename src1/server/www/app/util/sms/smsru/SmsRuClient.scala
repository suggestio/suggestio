package util.sms.smsru

import io.suggest.util.logs.MacroLogsImpl
import javax.inject.Inject
import models.sms.smsRu.MSmsRuResult
import models.sms.{ISmsSendResult, MSms}
import play.api.Configuration
import play.api.inject.Injector
import play.api.libs.ws.WSClient
import play.api.mvc.QueryStringBindable
import play.mvc.Http.HttpVerbs
import util.sms.ISmsSendClient

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 30.05.19 18:00
  * Description: Реализация клиента отправки сообщений для sms.ru.
  */
class SmsRuClient @Inject() (
                              injector                  : Injector,
                              wsClient                  : WSClient,
                              implicit private val ec   : ExecutionContext,
                            )
  extends ISmsSendClient
  with MacroLogsImpl
{

  import models.sms.smsRu.MSmsRuApi._


  /** id зарегистрированного приложения на sms.ru. */
  lazy val APP_ID: Option[String] = {
    injector
      .instanceOf[Configuration]
      .getOptional[String]( "sms.smsru.app.id" )
  }

  /** Ссылка для запроса отправки смс. */
  private def SEND_URL_PREFIX = "https://sms.ru/sms/send?"

  override def isReady(): Future[Boolean] =
    Future.successful( APP_ID.nonEmpty )


  /** Отправить смс на указанный номер (номера). */
  override def smsSend(sms: MSms): Future[ISmsSendResult] = {
    lazy val logPrefix = s"smsSend()#${System.currentTimeMillis()}:"

    // key выставляем в null, чтобы явно была ошибка при обращении к ключу с потолка.
    val url = SEND_URL_PREFIX + implicitly[QueryStringBindable[MSms]].unbind( null, sms )
    val method = HttpVerbs.POST
    LOGGER.trace(s"$logPrefix Sending sms[${sms.text.length} chars] to ${sms.numbers.size} numbers: ${sms.numbers.mkString(", ")}\n HTTP $method $url")

    for {
      wsResult <- injector
        .instanceOf[WSClient]
        .url( url )
        .withMethod( method )
        .withRequestTimeout( 10.seconds )
        .execute()
    } yield {
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
