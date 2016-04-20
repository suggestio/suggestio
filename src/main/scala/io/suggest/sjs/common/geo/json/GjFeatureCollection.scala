package io.suggest.sjs.common.geo.json

import io.suggest.sjs.common.model.FromDict

import scala.scalajs.js

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 11.04.16 16:13
  * Description: Модель GeoJSON features.
  */

object GjFeatureCollection extends FromDict {

  override type T = GjFeatureCollection

  def apply(features: js.Array[GjFeature] = js.Array()): GjFeatureCollection = {
    val fc = empty
    fc.`type` = GjTypes.FEATURE_COLLECTION
    fc.features = features
    fc
  }

}


@js.native
class GjFeatureCollection extends GjType {

  var features: js.Array[GjFeature] = js.native

}
