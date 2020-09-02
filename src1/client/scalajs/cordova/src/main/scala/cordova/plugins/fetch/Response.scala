package cordova.plugins.fetch

import org.scalajs.dom.experimental.ResponseType

import scala.scalajs.js

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 01.09.2020 21:06
  * Description: API for cordovaFetch.fetch() Response.
  */
@js.native
trait Response extends Body {

  val `type`: ResponseType = js.native

  // TODO Тут какой-то баг с status-полями: 409-код и другие не приходят в JS.
  val status: js.UndefOr[Int] = js.native
  val statusText: js.UndefOr[String] = js.native

  val ok: Boolean = js.native
  val headers: Headers = js.native
  val url: String = js.native

}
