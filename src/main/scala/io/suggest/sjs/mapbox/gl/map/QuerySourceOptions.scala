package io.suggest.sjs.mapbox.gl.map

import scala.scalajs.js
import scala.scalajs.js.UndefOr

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 12.04.16 14:50
  * Description: Options model API for map.querySourceFeatures().
  */
@js.native
class QuerySourceOptions extends js.Object {

  var sourceLayer: UndefOr[String] = js.native

  var filter: js.Array[js.Any] = js.native

}
