package io.suggest.sjs.common.model.loc

import io.suggest.geo.IGeoPoint
import io.suggest.geo.GeoConstants.Qs
import io.suggest.sjs.common.geo.json.{GjGeometry, GjTypes}
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

  def fromString(str: String): Option[MGeoPoint] = {
    str.split(Qs.LAT_LON_DELIM_FN) match {
      case Array(latStr, lonStr) =>
        try {
          val gp = MGeoPoint(
            lat = latStr.toDouble,
            lon = lonStr.toDouble
          )
          Some(gp)
        } catch { case ex: Throwable =>
          None
        }

      case other =>
        None
    }
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

  def toJsObject = js.Dictionary[Double](
    Qs.LAT_FN -> lat,
    Qs.LON_FN -> lon
  )

  def toGjPoint = GjGeometry(
    gtype       = GjTypes.Geom.POINT,
    coordinates = toJsArray
  )

  override def toString: String = {
    lat.toString + Qs.LAT_LON_DELIM_FN + lon.toString
  }

}
