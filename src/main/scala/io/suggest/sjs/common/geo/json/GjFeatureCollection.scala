package io.suggest.sjs.common.geo.json

import scala.scalajs.js

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 11.04.16 16:13
  * Description: Модель GeoJSON features.
  */

@js.native
class GjFeatureCollection extends GjType {

  var features: js.Array[GjFeature] = js.native

}
