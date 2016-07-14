package io.suggest.sc.sjs.m.mgeo

import io.suggest.geo.{GeoConstants, IGeoPoint}
import io.suggest.sjs.common.geo.json.{GjGeometry, GjTypes}
import io.suggest.sjs.mapbox.gl.ll.LngLat
import org.scalajs.dom.Coordinates

import scala.scalajs.js

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 22.05.15 9:48
 * Description: Реализация модели географической точки.
 */

object MGeoPoint {

  def apply(domCoords: Coordinates): MGeoPoint = {
    apply(
      lat = domCoords.latitude,
      lon = domCoords.longitude
    )
  }

  def apply(lngLat: LngLat): MGeoPoint = {
    apply(
      lat = lngLat.lat,
      lon = lngLat.lng
    )
  }

}

case class MGeoPoint(
  override val lat: Double,
  override val lon: Double
)
  extends IGeoPoint
{

  def toArray = js.Array[Double](lon, lat)
  def toJsArray = toArray.asInstanceOf[ js.Array[js.Any] ]

  def toJsObject = js.Dictionary[js.Any](
    GeoConstants.Qs.LAT_FN -> lat,
    GeoConstants.Qs.LON_FN -> lon
  )

  def toGjPoint = GjGeometry(
    gtype       = GjTypes.Geom.POINT,
    coordinates = toJsArray
  )

}
