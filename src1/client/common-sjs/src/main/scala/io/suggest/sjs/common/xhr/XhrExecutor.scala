package io.suggest.sjs.common.xhr

import org.scalajs.dom
import org.scalajs.dom.XMLHttpRequest

import scala.concurrent.{Future, Promise}
import scala.scalajs.js
import scala.scalajs.js.typedarray.ArrayBuffer
import japgolly.univeq._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 07.12.18 18:45
  * Description: Реализация HTTP-клиента поверх нативного XMLHttpRequest.
  */
case object XhrExecutor extends HttpClientExecutor {

  /** Считаем, что этот древний механизм всегда доступен. */
  override def isAvailable: Boolean = {
    try {
      !js.isUndefined( XMLHttpRequest )
    } catch {
      case _: Throwable => false
    }
  }


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

    new XhrHttpRespHolder( req, promise.future )
  }

}


/** Поддержка нативного XMLHttpRequest. */
class XhrHttpRespHolder(
                         xhr                  : XMLHttpRequest,
                         override val respFut : Future[XhrHttpResp]
                       )
  extends HttpRespHolder
{

  override def abortOrFail(): Unit =
    xhr.abort()

}


/** Реализация [[HttpResp]] для XHR-результата. */
case class XhrHttpResp( xhr: XMLHttpRequest ) extends HttpResp {
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
}

