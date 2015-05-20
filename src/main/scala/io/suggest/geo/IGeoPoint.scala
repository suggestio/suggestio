package io.suggest.geo

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

  override def toString: String = "GeoPoint(" + lat + "," + lon + ")"

  /** Сериализованное представление координат точки. */
  def toQsStr = lat.toString + "," + lon.toString

}
