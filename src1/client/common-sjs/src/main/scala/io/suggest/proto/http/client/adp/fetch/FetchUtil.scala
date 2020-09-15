package io.suggest.proto.http.client.adp.fetch

import io.suggest.pick.MimeConst
import io.suggest.proto.http.HttpConst
import io.suggest.proto.http.model.HttpReqAdp
import io.suggest.sjs.common.empty.JsOptionUtil
import io.suggest.sjs.dom2.FetchRequestInit
import japgolly.univeq._
import org.scalajs.dom.experimental.{AbortSignal, BodyInit, Headers, HttpMethod, RequestCredentials, RequestMode}

import scala.scalajs.js
import js.JSConverters._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 02.09.2020 18:16
  * Description:
  */
object FetchUtil {

  /** Сборка HttpReq в нативный RequestInit для fetch() или Request(). */
  def toRequestInit(req: HttpReqAdp, abortSignal: Option[AbortSignal]): FetchRequestInit = {
    var bodyUnd: js.UndefOr[BodyInit] = {
      val b = req.origReq.data.body
      JsOptionUtil.maybeDefined( b != null ) {
        b.asInstanceOf[BodyInit]
      }
    }

    // Нельзя использовать js.Array[js.Array[String]] для заголовков, т.к. CdvCordovaFetch отправляет чушь вместо заголовков.
    val reqHeaders = new Headers()
    for ((k,v) <- req.allReqHeaders)
      reqHeaders.append( k, v )

    val httpMethodStr = req.origReq
      .method
      .toUpperCase()

    if (
      req.origReq.data.config.forcePostBodyNonEmpty &&
      bodyUnd.isEmpty &&
      (httpMethodStr ==* HttpConst.Methods.POST)
    ) {
      bodyUnd = "": BodyInit

      // Выставить Content-Type, если отсутствует в заголовках:
      val ct = HttpConst.Headers.CONTENT_TYPE
      if (
        reqHeaders
          .get( ct )
          .toOption
          .flatMap( Option.apply )
          .fold(true)(_.isEmpty)
      )
        reqHeaders.append( ct, MimeConst.TEXT_PLAIN )
    }

    val _credentials = req.origReq.data.credentials
      .fold( RequestCredentials.`same-origin` ) {
        case true  => RequestCredentials.include
        case false => RequestCredentials.omit
      }

    new FetchRequestInit {
      override val method       = httpMethodStr.asInstanceOf[HttpMethod]
      override val headers      = reqHeaders
      override val body         = bodyUnd
      // TODO Передавать в реквесте? cors - дефолт.
      override val mode         = RequestMode.cors
      override val credentials  = _credentials
      override val signal       = abortSignal.orUndefined
    }
  }

}
