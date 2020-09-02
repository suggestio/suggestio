package cordova.plugins.fetch

import org.scalajs.dom.experimental.{HttpMethod, ReferrerPolicy, RequestCredentials, RequestMode}

import scala.scalajs.js

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 01.09.2020 21:01
  * Description:
  */
@js.native
trait Request extends Body {

  val url: String = js.native
  val credentials: RequestCredentials = js.native
  val headers: Headers = js.native
  val method: HttpMethod = js.native
  val mode: RequestMode = js.native
  val referrer: ReferrerPolicy = js.native

}
