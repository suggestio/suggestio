package io.suggest.sjs.dom2

import org.scalajs.dom.experimental.{AbortSignal, BodyInit, HeadersInit, HttpMethod, ReferrerPolicy, RequestCache, RequestCredentials, RequestInfo, RequestInit, RequestMode, RequestRedirect, Response}

import scala.scalajs.js
import scala.scalajs.js.annotation.JSGlobalScope

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 01.09.2020 11:14
  * Description: Замена RequestInit из-за неудобных var внутри.
  */
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
  val keepalive         : js.UndefOr[Boolean]             = js.undefined
  val signal            : js.UndefOr[AbortSignal]         = js.undefined
  val window            : js.UndefOr[Null]                = js.undefined
}
object FetchRequestInit {
  implicit class FriOpsExt(val fri: FetchRequestInit) extends AnyVal {
    def toDomRequestInit: RequestInit = fri.asInstanceOf[RequestInit]
  }
}


/** Поддержка Feature Detection на наличи Fetch API. */
@js.native
@JSGlobalScope
object FetchApiStub extends js.Object {
  val fetch: js.UndefOr[js.Function2[RequestInfo, RequestInit, js.Promise[Response]]] = js.native
}
