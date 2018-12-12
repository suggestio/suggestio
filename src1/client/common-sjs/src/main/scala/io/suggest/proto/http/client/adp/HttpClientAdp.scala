package io.suggest.proto.http.client.adp

import io.suggest.proto.http.model.{HttpFailedException, HttpRespHolder}
import io.suggest.proto.http.model.HttpReq
import io.suggest.sjs.common.async.AsyncUtil.defaultExecCtx

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

  /** Запустить http-запрос. */
  def apply(httpReq: HttpReq): HttpRespHolder

}

object HttpClientAdp {

  implicit class FutureOpsExt[T]( val fut: Future[T] ) extends AnyVal {

    /** Любой экзепшен нативного http-клиента надо отобразить в текущий формат. */
    def exception2httpEx(httpReq: HttpReq): Future[T] = {
      fut.recoverWith { case ex: Throwable =>
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
