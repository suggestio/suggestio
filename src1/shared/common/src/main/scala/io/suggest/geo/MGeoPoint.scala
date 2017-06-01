package io.suggest.geo

import boopickle.Default._
import io.suggest.common.html.HtmlConstants
import io.suggest.geo.GeoConstants.Qs

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 20.05.15 15:44
 * Description: Интерфейс для гео-точки.
 */
trait IGeoPoint {

  /** Долгота (X). */
  def lon: Double

  /** Широта (Y). */
  def lat: Double

  // TODO заменить на "lon|lat" ? Пользователю в браузере конечно удобенее "lat|lon", надо поразмыслить над этим.
  override def toString: String = {
    lat.toString + Qs.LAT_LON_DELIM_FN + lon.toString
  }

  /** Сериализованное представление координат точки. */
  // TODO Оно используется только в устаревшем GeoMode.
  def toQsStr = lat.toString + "," + lon.toString


  /** (12.1234 65.5633) */
  def toHumanFriendlyString: String = {
    def _fmt(coord: Double) = "%1.4f".format(coord)
    "(" + _fmt(lat) + HtmlConstants.SPACE + _fmt(lon) + ")"
  }

}


object MGeoPoint {

  implicit val pickler: Pickler[MGeoPoint] = generatePickler[MGeoPoint]

  /** Проверить точку на валидность координат. */
  def isValid(gp: MGeoPoint): Boolean = {
    Lat.isValid(gp.lat) && Lon.isValid(gp.lon)
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
        } catch { case _: Throwable =>
          None
        }

      case _ =>
        None
    }
  }

}


/** Дефолтовая, пошаренная между клиентом и сервером, реализация [[IGeoPoint]]. */
case class MGeoPoint(
                      // TODO Надо обменять порядок аргументов на (lon,lat).
                      // TODO Надо это учесть в FormUtil.geoPointM и в GeoPoint.FORMAT_OBJECT
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
