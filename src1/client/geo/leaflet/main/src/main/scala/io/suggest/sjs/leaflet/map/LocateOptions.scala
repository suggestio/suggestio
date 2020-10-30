package io.suggest.sjs.leaflet.map

import scala.scalajs.js

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 19.11.15 19:14
  * Description: Options for calling geolocation API.
  */
trait LocateOptions extends js.Object {

  val watch: js.UndefOr[Boolean] = js.undefined
  val setView: js.UndefOr[Boolean] = js.undefined
  val maxZoom: js.UndefOr[Double] = js.undefined
  val timeout: js.UndefOr[Double] = js.undefined
  val maximumAge: js.UndefOr[Double] = js.undefined
  val enableHighAccuracy: js.UndefOr[Boolean] = js.undefined

}
