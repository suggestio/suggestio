package io.suggest.proto.http.client.adp.xhr

import io.suggest.proto.http.client.adp.HttpClientAdp
import io.suggest.proto.http.model._
import io.suggest.sjs.JsApiUtil
import org.scalajs.dom
import org.scalajs.dom.{Blob, XMLHttpRequest}

import scala.concurrent.{Future, Promise}
import scala.scalajs.js.typedarray.ArrayBuffer
import japgolly.univeq._

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


  /** Запустить http-запрос. */
  override def apply(httpReq: HttpReq): XhrHttpRespHolder = {
    val req = new dom.XMLHttpRequest()
    val promise = Promise[XhrHttpResp]()
    val httpRes = XhrHttpResp(req)

    req.onreadystatechange = { (_: dom.Event) =>
      if (req.readyState ==* XMLHttpRequest.DONE) {
        // Если запрос невозможен (связи нет, например), то будет DONE и status=0
        if (req.status > 0)
          promise.success(httpRes)
        else promise.failure(
          HttpFailedException(
            url     = httpReq.url,
            method  = httpReq.method,
            resp    = Some(httpRes)
          )
        )
      }
    }

    req.open(httpReq.method, httpReq.url)
    val d = httpReq.data
    req.responseType = d.respType.xhrResponseType
    if (d.timeoutMsOr0 > 0)
      req.timeout = d.timeoutMsOr0
    req.withCredentials = d.xhrWithCredentialsCrossSite

    for (kv <- d.headers)
      req.setRequestHeader(kv._1, kv._2)

    if (d.body == null)
      req.send()
    else
      req.send(d.body)

    // Подцепить событие onProgress на функцию, если задана.
    for (onProgressF <- httpReq.data.onProgress) {
      req.onprogress = { e: dom.ProgressEvent =>
        // Скопипастить все данные в свой класс на случай mutable-инстансов событий, которые иногда встречаются в мире js.
        val progressInfo = MTransferProgressInfo(
          loadedBytes         = e.loaded,
          totalBytes          = e.total,
          lengthComputable    = e.lengthComputable,
        )
        onProgressF( progressInfo )
      }
    }

    new XhrHttpRespHolder( req, promise.future )
  }

}


/** Поддержка нативного XMLHttpRequest. */
class XhrHttpRespHolder(
                         xhr                  : XMLHttpRequest,
                         override val resultFut : Future[XhrHttpResp]
                       )
  extends HttpRespHolder
{

  override def abortOrFail(): Unit =
    xhr.abort()

}


/** Реализация [[HttpResp]] для XHR-результата. */
case class XhrHttpResp( xhr: XMLHttpRequest ) extends HttpResp {
  override def isFromInnerCache = false
  override def status = xhr.status
  override def statusText = xhr.statusText
  override def getHeader(headerName: String): Option[String] = {
    Option( xhr.getResponseHeader( headerName ) )
  }
  // Тело можно читать много раз, поэтому тут всегда false.
  override def bodyUsed = false
  override def text() =
    Future.successful( xhr.responseText )
  override def arrayBuffer() =
    Future.successful( xhr.response.asInstanceOf[ArrayBuffer] )
  override def blob() =
    Future.successful( xhr.response.asInstanceOf[Blob] )
}

