package io.suggest.sjs.common.geo.json

import scala.scalajs.js
import scala.scalajs.js.{Any, Dictionary, UndefOr}

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 11.04.16 16:11
  * Description: Модель GeoJSON Feature.
  *
  * Requires scala-js 0.6.14+.
  */

object GjFeature {

  def apply(fGeometry: GjGeometry,
            fProperties: UndefOr[Dictionary[Any]] = js.undefined): GjFeature = {
    new GjFeature {
      override val `type`       = GjTypes.FEATURE
      override val geometry     = fGeometry
      override val properties   = fProperties
    }
  }

}


trait GjFeature extends GjType {

  val properties: UndefOr[ Dictionary[Any] ] = js.undefined

  val geometry: GjGeometry

}
