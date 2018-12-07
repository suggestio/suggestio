package io.suggest.sjs.common.xhr

import io.suggest.sjs.common.async.AsyncUtil.defaultExecCtx
import io.suggest.sjs.common.empty.JsOptionUtil
import org.scalajs.dom.experimental._

import scala.concurrent.Future
import scala.scalajs.js
import scala.scalajs.js.JSConverters._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 07.12.18 20:29
  * Description: Xhr-адаптер для Fetch API.
  */
case object FetchExecutor extends HttpClientExecutor {

  import HttpClientExecutor._

  /** Доступно ли указанное API? */
  override def isAvailable: Boolean = {
    try {
      Fetch.asInstanceOf[FetchApiStub].fetch.nonEmpty
    } catch {
      case _: Throwable => false
    }
  }

  /** Запустить http-запрос. */
  override def apply(httpReq: HttpReq): HttpRespHolder = {
    val reqInit: FetchRequestInit = new FetchRequestInit {
      override val method = httpReq.method.toUpperCase().asInstanceOf[HttpMethod]
      override val headers: js.UndefOr[HeadersInit] = {
        val hs = httpReq.data.headers
        JsOptionUtil.maybeDefined( hs.nonEmpty ) {
          hs.iterator
            .map { case (k, v) =>
              js.Array(k, v)
            }
            .toJSArray: HeadersInit
        }
      }
      override val body: js.UndefOr[BodyInit] = {
        val b = httpReq.data.body
        JsOptionUtil.maybeDefined( b != null ) {
          b.asInstanceOf[BodyInit]
        }
      }
      override val mode = RequestMode.cors    // TODO Передавать в реквесте? cord - дефолт.
      override val credentials = RequestCredentials.`same-origin`
    }

    val respFut = Fetch.fetch(
      httpReq.url,
      reqInit.toRequestInit
    )
      .toFuture
      // Любой экзепшен отобразить в текущий формат.
      .exception2httpEx(httpReq)

    FetchHttpRespHolder( respFut )
  }

}


/** Замена RequestInit из-за неудобных var внутри. */
trait FetchRequestInit extends js.Object {
  val method            : js.UndefOr[HttpMethod]          = js.undefined
  val headers           : js.UndefOr[HeadersInit]         = js.undefined
  val body              : js.UndefOr[BodyInit]            = js.undefined
  val referrer          : js.UndefOr[String]              = js.undefined
  val referrerPolicy    : js.UndefOr[ReferrerPolicy]      = js.undefined
  val mode              : js.UndefOr[RequestMode]         = js.undefined
  val credentials       : js.UndefOr[RequestCredentials]  = js.undefined
  val requestCache      : js.UndefOr[RequestCache]        = js.undefined
  val requestRedirect   : js.UndefOr[RequestRedirect]     = js.undefined
  val integrity         : js.UndefOr[String]              = js.undefined
  val window            : js.UndefOr[Null]                = js.undefined
}
object FetchRequestInit {
  implicit class FriOpsExt(val fri: FetchRequestInit) extends AnyVal {
    def toRequestInit: RequestInit = fri.asInstanceOf[RequestInit]
  }
}


/** Поддержка Feature Detection на наличи Fetch API. */
@js.native
trait FetchApiStub extends js.Object {
  val fetch: js.UndefOr[js.Function] = js.native
}


/** Реализация [[HttpRespHolder]] над Fetch API.
  * @param fetchRespFut Фьючерс ответа.
  */
case class FetchHttpRespHolder(
                                fetchRespFut: Future[Response]
                              )
  extends HttpRespHolder
{

  def withRespFut(respFut: Future[Response]) = copy(fetchRespFut = respFut)

  override lazy val respFut: Future[HttpResp] = {
    for (resp <- fetchRespFut) yield
      FetchHttpResp( resp )
  }

}


/** Реализация [[HttpResp]] для Fetch-результатов. */
case class FetchHttpResp( resp: Response ) extends HttpResp {
  override def status = resp.status
  override def statusText = resp.statusText
  override def getHeader(headerName: String): Option[String] = {
    resp.headers
      .get( headerName )
      .toOption
      // фильтруем null внутри undefined
      .flatMap( Option.apply )
  }
  override def bodyUsed = resp.bodyUsed
  override def text() = resp.text().toFuture
  override def arrayBuffer() = resp.arrayBuffer().toFuture
}
