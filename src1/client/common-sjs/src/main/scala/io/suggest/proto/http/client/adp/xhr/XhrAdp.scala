package io.suggest.proto.http.client.adp.xhr

import io.suggest.log.Log
import io.suggest.msg.ErrorMsgs
import io.suggest.proto.http.client.adp.fetch.FetchUtil
import io.suggest.proto.http.client.adp.{HttpAdpInstance, HttpClientAdp}
import io.suggest.proto.http.model._
import io.suggest.sjs.JsApiUtil
import io.suggest.up.ITransferProgressInfo
import org.scalajs.dom
import org.scalajs.dom.{Blob, XMLHttpRequest}

import scala.concurrent.{Future, Promise}
import scala.scalajs.js.typedarray.ArrayBuffer
import japgolly.univeq._
import org.scalajs.dom.experimental.{BodyInit, Headers, RequestInit, Response, ResponseInit}

import scala.util.Try

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 07.12.18 18:45
  * Description: Реализация HTTP-клиента поверх нативного XMLHttpRequest.
  */
case object XhrAdp extends HttpClientAdp {

  /** Считаем, что этот древний механизм всегда доступен. */
  override def isAvailable: Boolean =
    JsApiUtil.isDefinedSafe( XMLHttpRequest )

  override def factory = XhrAdpInstance

}


/** Инстанс для запроса через XHR-адаптер. */
case class XhrAdpInstance( override val httpReq: HttpReqAdp ) extends HttpAdpInstance {

  val xhr = new dom.XMLHttpRequest()

  override lazy val toRequestInit: RequestInit = {
    FetchUtil
      .toRequestInit( httpReq, abortSignal = None )
      .toDomRequestInit
  }

  override def doRequest(requestUrl: String): Future[HttpResp] = {
    val promise = Promise[XhrHttpResp]()
    val httpRes = XhrHttpResp(xhr)

    xhr.onreadystatechange = { (_: dom.Event) =>
      if (xhr.readyState ==* XMLHttpRequest.DONE) {
        // Если запрос невозможен (связи нет, например), то будет DONE и status=0
        if (xhr.status > 0)
          promise.success(httpRes)
        else promise.failure(
          HttpFailedException(
            url     = httpReq.origReq.url,
            method  = httpReq.origReq.method,
            resp    = Some(httpRes)
          )
        )
      }
    }

    xhr.open( httpReq.origReq.method, httpReq.reqUrl )
    val d = httpReq.origReq.data
    xhr.responseType = d.respType.xhrResponseType
    if (d.timeoutMsOr0 > 0)
      xhr.timeout = d.timeoutMsOr0
    xhr.withCredentials = d.credentials contains true

    for (kv <- httpReq.allReqHeaders)
      xhr.setRequestHeader(kv._1, kv._2)

    if (d.body == null)
      xhr.send()
    else
      xhr.send(d.body)

    // Подцепить событие onProgress на функцию, если задана.
    for (onProgressF <- httpReq.origReq.data.onProgress) {
      xhr.onprogress = { e: dom.ProgressEvent =>
        val progressInfo = new ITransferProgressInfo {
          def loadedBytes         = e.loaded
          def totalBytes          = Option.when(e.lengthComputable)( e.total )
        }
        onProgressF( progressInfo )
      }
    }

    promise.future
  }

  override def toRespHolder(respFut: Future[HttpResp]) =
    XhrHttpRespHolder(xhr, respFut)

}


/** Поддержка нативного XMLHttpRequest. */
case class XhrHttpRespHolder(
                              xhr                     : XMLHttpRequest,
                              override val resultFut  : Future[HttpResp],
                            )
  extends IHttpRespHolder
{

  override def abortOrFail(): Unit =
    xhr.abort()

}


/** Реализация [[HttpResp]] для XHR-результата. */
case class XhrHttpResp( xhr: XMLHttpRequest ) extends HttpResp with Log { outer =>
  override def isFromInnerCache = false
  override def status = xhr.status
  override def statusText = xhr.statusText
  override def getHeader(headerName: String): Seq[String] = {
    Option( xhr.getResponseHeader( headerName ) )
      .toList
  }
  // Тело можно читать много раз, поэтому тут всегда false.
  override def bodyUsed = false
  override def text() =
    Future.successful( xhr.responseText )
  override def arrayBuffer() =
    Future.successful( xhr.response.asInstanceOf[ArrayBuffer] )
  override def blob() =
    Future.successful( xhr.response.asInstanceOf[Blob] )

  override def headers: Iterator[(String, String)] = {
    xhr
      .getAllResponseHeaders()
      .trim
      .split( "[\r\n]+" )
      .iterator
      .map { line =>
        val Array(k, v) = line.split(": ")
        (k, v)
      }
  }

  /** Конверсия XHR-ответа в стандартный DOM Fetch Response. */
  override def toDomResponse(): Option[Response] = {
    val respHdrs = new Headers()
    for (kv @ (k,v) <- headers)
      for (ex <- Try(respHdrs.append(k, v)).failed)
        logger.warn( ErrorMsgs.HTTP_HEADER_PROBLEM, ex, kv )

    val domResp = new Response(
      content = xhr.response.asInstanceOf[BodyInit],
      init = new ResponseInit {
        override var status     = outer.status
        override var statusText = outer.statusText
        override var headers    = respHdrs
      }
    )

    Some(domResp)
  }

}

