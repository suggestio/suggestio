package io.suggest.sjs.common.geo.json

import io.suggest.sjs.common.model.FromDict

import scala.scalajs.js
import scala.scalajs.js.{Any, Dictionary, UndefOr}

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 11.04.16 16:11
  * Description: Модель GeoJSON Feature.
  */

object GjFeature extends FromDict {

  override type T = GjFeature

  def apply(geometry: GjGeometry, properties: UndefOr[Dictionary[Any]] = js.undefined): GjFeature = {
    val gf = empty
    gf.`type` = GjTypes.FEATURE
    gf.geometry = geometry
    if (properties.nonEmpty)
      gf.properties = properties
    gf
  }

}


@js.native
class GjFeature extends GjType {

  var properties: UndefOr[ Dictionary[Any] ] = js.native

  var geometry: GjGeometry = js.native

}
