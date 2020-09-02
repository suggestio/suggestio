package io.suggest.proto.http.client.adp.fetch

import io.suggest.proto.http.client.adp.{HttpAdpInstance, HttpClientAdp}
import io.suggest.proto.http.model._
import io.suggest.sjs.JsApiUtil
import io.suggest.sjs.common.async.AsyncUtil.defaultExecCtx
import io.suggest.sjs.dom2._
import org.scalajs.dom.experimental._

import scala.concurrent.Future
import scala.util.Try

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 07.12.18 20:29
  * Description: Xhr-адаптер для Fetch API.
  */
case object FetchAdp extends HttpClientAdp {

  /** Доступно ли указанное API? */
  override def isAvailable: Boolean =
    JsApiUtil.isDefinedSafe( FetchApiStub.fetch )

  /** Сборка инстанса подготовки к работе под указанный реквест. */
  override def factory = FetchAdpInstance

  /** Обёртка для стандартной fetch-фунции */
  def _nativeFetchF = {
    FetchApiStub
      .fetch
      .toOption
      .map { nativeFetchF =>
        nativeFetchF(_: RequestInfo, _: RequestInit)
          .toFuture
          .map { httpResp =>
            FetchHttpResp(httpResp, isFromInnerCache = false)
          }
      }
  }

}


/** Контекст обработки для одного реквеста через fetch API. */
case class FetchAdpInstance( override val httpReq: HttpReqAdp ) extends HttpAdpInstance {

  import HttpClientAdp._

  val abortCtl = Try( new AbortController() )
    .toOption

  def abortSignal = abortCtl.map(_.signal)

  override lazy val toRequestInit: RequestInit =
    FetchUtil.toRequestInit( httpReq, abortSignal ).toDomRequestInit

  override def doRequest(requestUrl: String): Future[HttpResp] = {
    httpReq.origReq.data.config.fetchApi
      .orElse( FetchAdp._nativeFetchF )
      // .get.apply() - тут принудительно раскрывается Option.
      .get( requestUrl, toRequestInit )
      // Любой экзепшен отобразить в текущий формат.
      .exception2httpEx( httpReq.origReq )
  }

  override def toRespHolder(respFut: Future[HttpResp]): IHttpRespHolder =
    FetchHttpRespHolder( abortCtl, respFut )

}


/** Реализация [[IHttpRespHolder]] над Fetch API.
 *
  * @param resultFut Фьючерс ответа.
  */
case class FetchHttpRespHolder(
                                abortCtlUnd             : Option[AbortController],
                                override val resultFut  : Future[HttpResp],
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
  override def getHeader(headerName: String): Seq[String] = {
    resp.headers
      .getAllUnd
      .fold [Seq[String]] {
        // Фунция getAll() недоступна в данном браузере.
        Option(
          resp.headers
            .get( headerName )
            .orNull
        )
          .toList
      } { _ =>
        resp.headers
          .getAll(headerName)
          .toSeq
      }
  }

  // Тут forEach O(n) с пожиранием ресурсов из-за непонятков с API cordova-fetch.
  private lazy val _headersForEached: List[(String, String)] = {
    Try {
      var acc = List.empty[(String, String)]

      // Если cordova-plugin-fetch, то для прохода списка годится только forEach().
      // Из-за зоопарка реализаций, get()/getAll() и не всегда доступного iterator'а, тут используем forEach:
      resp
        .headers
        .forEach { (value, name) =>
          val kv = name -> value
          acc ::= kv
        }

      acc
    }
      .getOrElse( Nil )
  }

  override def headers: IterableOnce[(String, String)] = {
    Try {
      for {
        // .iterator: будет exception в cordova-fetch, т.к. там нет итератора.
        hdrArr <- resp.headers.iterator
        k = hdrArr.shift()
        v <- hdrArr.iterator
      } yield {
        k -> v
      }
    }
      // Попытаться залезть в приватное поле headers.map:
      .orElse {
        Try {
          resp.headers
            ._cdvFetchHeadersMap
            .get
            .iterator
            .flatMap { case (k, vs) =>
              vs.iterator
                .map(v => k -> v)
            }
        }
      }
      // Попытаться извлечь заголовке через forEach().
      .getOrElse( _headersForEached )
  }

  override def bodyUsed = resp.bodyUsed
  override def text() = resp.text().toFuture
  override def arrayBuffer() = resp.arrayBuffer().toFuture
  override def blob() = resp.blob().toFuture

  override def toDomResponse() = Some(resp)

}
