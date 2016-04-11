package io.suggest.sjs.common.geo.json

import scala.scalajs.js
import scala.scalajs.js.{Any, Dictionary, UndefOr}

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 11.04.16 16:11
  * Description: Модель GeoJSON Feature.
  */

class GjFeature extends GjType {

  var properties: UndefOr[ Dictionary[Any] ] = js.native

  var geometry: Dictionary[Any] = js.native

}
