package io.suggest.geo

import io.suggest.sjs.common.geo.json.{GjCoord_t, GjGeometry, GjTypes}
import org.scalajs.dom.Coordinates

import scala.scalajs.js

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 22.05.15 9:48
  * Description: js-поддержка модели географической точки.
  */

object MGeoPointJs {

  def apply(domCoords: Coordinates): MGeoPoint = {
    MGeoPoint(
      lat = domCoords.latitude,
      lon = domCoords.longitude
    )
  }

  /** Массив координат в стандартной нотации: [lon,lat] т.е. [x,y]. */
  def toArray(gp: IGeoPoint): GjCoord_t = js.Array[Double](gp.lon, gp.lat)

  def toJsArray(gp: IGeoPoint) = toArray(gp)//.asInstanceOf[ js.Array[js.Any] ]

  def fromGjArray(arr: js.Array[Double]): MGeoPoint = {
    MGeoPoint(
      lon = arr(0),
      lat = arr(1)
    )
  }

  /** leaflet использовать массивы в традиционной нотации: [y, x] то бишь [lat, lon]. */
  def toLatLngArray(gp: IGeoPoint) = js.Array[Double](gp.lat, gp.lon)

  def toGjPoint(gp: IGeoPoint): GjGeometry = {
    GjGeometry(
      gtype        = GjTypes.Geom.POINT,
      gcoordinates = toJsArray(gp)
    )
  }

  def toJsObject(gp: IGeoPoint) = js.Dictionary[Double](
    Lat.QS_FN -> gp.lat,
    Lon.QS_FN -> gp.lon
  )

}
