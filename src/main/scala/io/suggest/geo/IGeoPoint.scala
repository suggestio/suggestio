package io.suggest.geo

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



/** Дефолтовая, пошаренная между клиентом и сервером, реализация [[IGeoPoint]]. */
case class MGeoPoint(
  override val lat: Double,
  override val lon: Double
)
  extends IGeoPoint
