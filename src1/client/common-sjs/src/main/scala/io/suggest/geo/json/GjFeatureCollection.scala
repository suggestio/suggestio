package io.suggest.geo.json

import scala.scalajs.js

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 11.04.16 16:13
  * Description: Модель GeoJSON features.
  */

object GjFeatureCollection {

  def apply(fcFeatures: js.Array[GjFeature] = js.Array()): GjFeatureCollection = {
    new GjFeatureCollection {
      override val `type`   = GjTypes.FEATURE_COLLECTION
      override val features = fcFeatures
    }
  }

}


trait GjFeatureCollection extends GjType {

  val features: js.Array[GjFeature]

}
