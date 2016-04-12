package io.suggest.sjs.mapbox.gl.layer

import io.suggest.sjs.common.geo.json.GjFeature

import scala.scalajs.js

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 12.04.16 14:48
  * Description: API for map.queryRenderedFeatures() results.
  */
@js.native
class LayerFeature extends GjFeature {

  var layer: ILayer = js.native

}
