package io.suggest.geo

import boopickle.Default._
import io.suggest.geo.GeoConstants.Qs

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 20.05.15 15:44
 * Description: Интерфейс для гео-точки.
 */
trait IGeoPoint {

  /** Широта. */
  def lat: Double

  /** Долгота. */
  def lon: Double

  override def toString: String = {
    lat.toString + Qs.LAT_LON_DELIM_FN + lon.toString
  }

  /** Сериализованное представление координат точки. */
  // TODO Оно используется только в устаревшем GeoMode.
  def toQsStr = lat.toString + "," + lon.toString

}


object MGeoPoint {

  implicit val pickler: Pickler[MGeoPoint] = generatePickler[MGeoPoint]

}


/** Дефолтовая, пошаренная между клиентом и сервером, реализация [[IGeoPoint]]. */
case class MGeoPoint(
  override val lat: Double,
  override val lon: Double
)
  extends IGeoPoint
{

  def withLat(lat2: Double) = copy(lat = lat2)
  def withLon(lon2: Double) = copy(lon = lon2)

}


/** Интерфейс для моделей с полем geoPoint. */
trait IGeoPointField {
  def geoPoint: MGeoPoint
}
