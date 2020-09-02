package cordova.plugins.fetch

import org.scalajs.dom.experimental.{RequestInfo, RequestInit}

import scala.scalajs.js
import scala.scalajs.js.annotation.{JSGlobalScope, JSName}

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 01.09.2020 11:25
  * Description: Cordova-Fetch API.
  */
@js.native
@JSGlobalScope
object CdvPluginFetch extends js.Object {

  // Копипаст fetch() из org.scalajs.dom.experimental.Fetch:
  def cordovaFetch(info: RequestInfo,
                   init: RequestInit = null): js.Promise[Response] = js.native

  /** undefined+API для feature-detection. */
  @JSName("cordovaFetch")
  val cordovaFetchUnd: js.UndefOr[js.Function2[RequestInfo, RequestInit, js.Promise[Response]]] = js.native

}
