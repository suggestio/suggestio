package io.suggest.sc.sjs.m.mgeo

import io.suggest.geo.MGeoPoint
import io.suggest.sjs.mapbox.gl.ll.LngLat

import scala.scalajs.js

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 07.10.16 15:47
  * Description: Расширенные методы сборки MGeoPoint.
  */
object MGeoPointExt {

  def apply(lngLat: LngLat): MGeoPoint = {
    MGeoPoint(
      lat = lngLat.lat,
      lon = lngLat.lng
    )
  }

  def apply(lonLat: js.Array[_]): MGeoPoint = {
    val s = LngLat.convert( lonLat.asInstanceOf[js.Array[js.Any]] )
    apply(s)
  }

}
