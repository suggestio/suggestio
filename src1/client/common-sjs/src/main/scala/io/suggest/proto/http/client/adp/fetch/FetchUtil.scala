package io.suggest.proto.http.client.adp.fetch

import io.suggest.proto.http.model.HttpReqAdp
import io.suggest.sjs.common.empty.JsOptionUtil
import io.suggest.sjs.dom2.FetchRequestInit
import org.scalajs.dom.experimental.{AbortSignal, BodyInit, Headers, HeadersInit, HttpMethod, RequestCredentials, RequestMode}

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
    val bodyUnd: js.UndefOr[BodyInit] = {
      val b = req.origReq.data.body
      JsOptionUtil.maybeDefined( b != null ) {
        b.asInstanceOf[BodyInit]
      }
    }

    // Нельзя использовать js.Array[js.Array[String]] для заголовков, т.к. CdvCordovaFetch отправляет чушь вместо заголовков.
    val reqHeaders = new Headers()
    for ((k,v) <- req.allReqHeaders)
      reqHeaders.append( k, v )

    val httpMethod = req.origReq
      .method
      .toUpperCase()
      .asInstanceOf[HttpMethod]

    val _credentials = req.origReq.data.credentials
      .fold( RequestCredentials.`same-origin` ) {
        case true  => RequestCredentials.include
        case false => RequestCredentials.omit
      }

    new FetchRequestInit {
      override val method       = httpMethod
      override val headers      = reqHeaders
      override val body         = bodyUnd
      // TODO Передавать в реквесте? cors - дефолт.
      override val mode         = RequestMode.cors
      override val credentials  = _credentials
      override val signal       = abortSignal.orUndefined
    }
  }

}
