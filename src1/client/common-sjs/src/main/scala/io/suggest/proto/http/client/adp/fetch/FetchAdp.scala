package io.suggest.proto.http.client.adp.fetch

import io.suggest.proto.http.client.adp.HttpClientAdp
import io.suggest.proto.http.client.cache.HttpCaching
import io.suggest.proto.http.model._
import io.suggest.sjs.JsApiUtil
import io.suggest.sjs.common.async.AsyncUtil.defaultExecCtx
import io.suggest.sjs.common.empty.JsOptionUtil
import io.suggest.sjs.common.empty.JsOptionUtil.Implicits._
import io.suggest.sjs.dom2.{AbortController, AbortControllerUtil, AbortSignal}
import org.scalajs.dom.experimental._

import scala.concurrent.Future
import scala.scalajs.js
import scala.scalajs.js.JSConverters._
import scala.scalajs.js.annotation.JSGlobalScope

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 07.12.18 20:29
  * Description: Xhr-адаптер для Fetch API.
  */
case object FetchAdp extends HttpClientAdp {

  import HttpClientAdp._

  /** Доступно ли указанное API? */
  override def isAvailable: Boolean =
    JsApiUtil.isDefinedSafe( FetchApiStub.fetch )

  /** Запустить http-запрос. */
  override def apply(httpReq: HttpReq): FetchHttpRespHolder = {
    // Abort-контроллер, поддержка которого может отсутствовать.
    val abortCtlUnd = JsOptionUtil.maybeDefined( AbortControllerUtil.isAvailable() ) {
      new AbortController()
    }
    val abortSignalUnd = abortCtlUnd.map(_.signal)
    val reqInit = HttpFetchUtil.toRequestInit( httpReq, abortSignalUnd )

    // Ответ сервера - через собственный кэш:
    val respFut = HttpCaching.processCaching(httpReq, reqInit) { request =>
      Fetch
        .fetch( request )
        .toFuture
        .map { FetchHttpResp(_, isFromInnerCache = false) }
        // Любой экзепшен отобразить в текущий формат.
        .exception2httpEx(httpReq)
    }

    // TODO Поддержка progress'а (для upload'а).
    //   Для Fetch API требуются вкручивать костыли по-интереснее, что потребует изменения HttpClient API (upload progress надо будет заявлять при сборке реквеста):
    //   - https://github.com/whatwg/fetch/issues/21#issuecomment-381425704 (отсылка к спекам, требуется Request поверх ReadableStream в браузере).
    //   - https://github.com/whatwg/fetch/issues/607#issuecomment-564461907 (download-пример, НЕ для upload)
    // Поэтому происходит откат XhrAdp при аплоаде с progress-функцией.

    new FetchHttpRespHolder( abortCtlUnd, respFut )
  }

}


/** Статическая утиль для Fetch API. */
object HttpFetchUtil {

  /** Сборка HttpReq в нативный RequestInit для fetch() или Request(). */
  def toRequestInit(httpReq: HttpReq, abortSignalUnd: js.UndefOr[AbortSignal] = js.undefined): FetchRequestInit = {
    val bodyUnd: js.UndefOr[BodyInit] = {
      val b = httpReq.data.body
      JsOptionUtil.maybeDefined( b != null ) {
        b.asInstanceOf[BodyInit]
      }
    }

    val reqHeaders: js.UndefOr[HeadersInit] = {
      val hs = httpReq.data.allHeaders

      JsOptionUtil.maybeDefined( hs.nonEmpty ) {
        hs.iterator
          .map { case (k, v) =>
            js.Array(k, v)
          }
          .toJSArray: HeadersInit
      }
    }

    val httpMethod = httpReq
      .method
      .toUpperCase()
      .asInstanceOf[HttpMethod]

    new FetchRequestInit {
      override val method       = httpMethod
      override val headers      = reqHeaders
      override val body         = bodyUnd
      // TODO Передавать в реквесте? cors - дефолт.
      override val mode         = RequestMode.cors
      override val credentials  = RequestCredentials.`same-origin`
      val signal                = abortSignalUnd
    }
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
@JSGlobalScope
object FetchApiStub extends js.Object {
  val fetch: js.UndefOr[js.Function] = js.native
}


/** Реализация [[IHttpRespHolder]] над Fetch API.
 *
  * @param resultFut Фьючерс ответа.
  */
case class FetchHttpRespHolder(
                                abortCtlUnd          : js.UndefOr[AbortController],
                                override val resultFut : Future[FetchHttpResp]
                              )
  extends IHttpRespHolder
{

  /** Прервать запрос. */
  override def abortOrFail(): Unit = {
    for (abortCtl <- abortCtlUnd)
      abortCtl.abort()
  }

}


/** Реализация [[HttpResp]] для Fetch-результатов. */
case class FetchHttpResp(
                          resp: Response,
                          override val isFromInnerCache: Boolean,
                        )
  extends HttpResp
{
  override def status = resp.status
  override def statusText = resp.statusText
  override def getHeader(headerName: String): Option[String] = {
    resp.headers
      .get( headerName )
      .toOptionNullable
  }
  override def bodyUsed = resp.bodyUsed
  override def text() = resp.text().toFuture
  override def arrayBuffer() = resp.arrayBuffer().toFuture
  override def blob() = resp.blob().toFuture
}
