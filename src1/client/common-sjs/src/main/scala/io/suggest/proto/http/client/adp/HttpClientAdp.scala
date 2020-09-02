package io.suggest.proto.http.client.adp

import io.suggest.i18n.MsgCodes
import io.suggest.log.Log
import io.suggest.proto.http.model.{HttpFailedException, HttpReq, HttpReqAdp, HttpResp, IHttpRespHolder}
import io.suggest.sjs.common.async.AsyncUtil.defaultExecCtx
import org.scalajs.dom.experimental.RequestInit

import scala.concurrent.Future

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 12.12.18 10:43
  * Description: Интерфейс для унифицированных http-client-адаптеров нативных API.
  */
trait HttpClientAdp {

  /** Доступно ли указанное API? */
  def isAvailable: Boolean

  /** Сборка инстанса подготовки к работе под указанный реквест. */
  def factory: (HttpReqAdp) => HttpAdpInstance

}


/** Интерфейс для реализации состояния обработки одного запроса.
  * Нужен для поддержки client-side кэширования ответов. */
trait HttpAdpInstance {

  /** Данные исходного реквеста. */
  val httpReq: HttpReqAdp

  /** Сборка инстанса RequestInit. */
  def toRequestInit: RequestInit

  /** Запуск реквеста на исполнение.
    *
    * @param requestUrl Т.к. ссылка может переопределяться под нужды кэша или конфига, она передаётся отдельно.
    * @return Фьючерс с запущенным на исполнение реквестом.
    */
  def doRequest(requestUrl: String = httpReq.reqUrl): Future[HttpResp]

  /** Завернуть фьючерс ответа сервера в конкретную реализацию RespHolder'а.
    *
    * @param respFut Фьючерс ответа сервера.
    * @return Конкретная реализация RespHolder.
    */
  def toRespHolder(respFut: Future[HttpResp]): IHttpRespHolder

}


object HttpClientAdp extends Log {

  implicit final class FutureOpsExt[T]( val fut: Future[T] ) extends AnyVal {

    /** Любой экзепшен нативного http-клиента надо отобразить в текущий формат. */
    def exception2httpEx(httpReq: HttpReq): Future[T] = {
      fut.recoverWith { case ex: Throwable =>
        logger.warn( MsgCodes.`Send.request`, ex, httpReq )
        val ex9 = ex match {
          case _: HttpFailedException =>
            ex
          case _ =>
            HttpFailedException(
              url       = httpReq.url,
              method    = httpReq.method,
              getCause  = ex
            )
        }
        Future.failed(ex9)
      }
    }

  }

}
