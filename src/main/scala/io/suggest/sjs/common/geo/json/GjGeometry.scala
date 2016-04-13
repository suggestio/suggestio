package io.suggest.sjs.common.geo.json

import io.suggest.sjs.common.model.FromDict

import scala.scalajs.js

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 13.04.16 21:36
  * Description: Модель абстрактной геометрии GeoJSON.
  */
object GjGeometry extends FromDict {

  override type T = GjGeometry

  def apply(gtype: String, coordinates: js.Array[js.Any]): GjGeometry = {
    val g = empty
    g.`type` = gtype
    g.coordinates = coordinates
    g
  }

}


@js.native
class GjGeometry extends GjType {

  var coordinates: js.Array[js.Any] = js.native

}
