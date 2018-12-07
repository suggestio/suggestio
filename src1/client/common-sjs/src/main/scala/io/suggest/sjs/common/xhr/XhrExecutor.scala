package io.suggest.sjs.common.xhr

import io.suggest.sjs.common.async.AsyncUtil.defaultExecCtx
import org.scalajs.dom.XMLHttpRequest
import org.scalajs.dom.ext.{Ajax, AjaxException}

import scala.concurrent.Future
import scala.scalajs.js
import scala.scalajs.js.typedarray.ArrayBuffer

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 07.12.18 18:45
  * Description: Реализация HTTP-клиента поверх нативного XMLHttpRequest.
  */
case object XhrExecutor extends HttpClientExecutor {

  import HttpClientExecutor._

  /** Считаем, что этот древний механизм всегда доступен. */
  override def isAvailable: Boolean = {
    try {
      !js.isUndefined( XMLHttpRequest )
    } catch {
      case _: Throwable => false
    }
  }


  /** Запустить http-запрос. */
  override def apply(httpReq: HttpReq): HttpRespHolder = {
    val xhrFut = Ajax(
      method          = httpReq.method,
      url             = httpReq.url,
      data            = httpReq.data.body,
      timeout         = httpReq.data.timeoutMsOr0,
      headers         = httpReq.data.headers,
      withCredentials = httpReq.data.xhrWithCredentialsCrossSite,
      responseType    = httpReq.data.respType.xhrResponseType,
    )
      .exception2httpEx(httpReq)

    XhrHttpRespHolder( xhrFut )
  }

}


/** Поддержка нативного XMLHttpRequest. */
case class XhrHttpRespHolder(
                              xhrFut: Future[XMLHttpRequest]
                            )
  extends HttpRespHolder
{

  def withXhrFut(xhrFut: Future[XMLHttpRequest]) = copy(xhrFut = xhrFut)

  override lazy val respFut: Future[HttpResp] = {
    for (xhr <- xhrFut) yield
      XhrHttpResp( xhr )
  }

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

