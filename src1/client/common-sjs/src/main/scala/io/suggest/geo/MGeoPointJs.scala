package io.suggest.geo

import io.suggest.geo.json._
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
    MGeoPoint.fromDouble(
      lat = domCoords.latitude,
      lon = domCoords.longitude
    )
  }

  /** Массив координат в стандартной нотации: [lon,lat] т.е. [x,y]. */
  def toArray(gp: MGeoPoint): GjCoord_t = js.Array[Double](gp.lon.doubleValue, gp.lat.doubleValue)

  def toJsArray(gp: MGeoPoint) = toArray(gp)//.asInstanceOf[ js.Array[js.Any] ]

  def fromGjArray(arr: js.Array[Double]): MGeoPoint = {
    MGeoPoint.fromDouble(
      lon = arr(0),
      lat = arr(1)
    )
  }

  /** leaflet использовать массивы в традиционной нотации: [y, x] то бишь [lat, lon]. */
  def toLatLngArray(gp: MGeoPoint) = js.Array[Double](gp.lat.doubleValue, gp.lon.doubleValue)

  def toGjPoint(gp: MGeoPoint): GjGeometry = {
    GjGeometry(
      gtype        = GjTypes.Geom.POINT,
      gcoordinates = toJsArray(gp)
    )
  }

  def toJsObject(gp: MGeoPoint) = js.Dictionary[Double](
    Lat.QS_FN -> gp.lat.doubleValue,
    Lon.QS_FN -> gp.lon.doubleValue
  )

}
