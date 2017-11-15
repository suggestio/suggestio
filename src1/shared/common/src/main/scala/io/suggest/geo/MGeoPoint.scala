package io.suggest.geo

import boopickle.Default._
import io.suggest.common.html.HtmlConstants
import io.suggest.geo.GeoConstants.Qs
import japgolly.univeq.UnivEq

import scalaz.ValidationNel
import scalaz.syntax.apply._

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

  implicit val MGEO_POINT_PICKLER: Pickler[MGeoPoint] = generatePickler[MGeoPoint]

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


  def validator(mgp: MGeoPoint): ValidationNel[String, MGeoPoint] = {
    (
      Lat.validator( mgp.lat ) |@|
      Lon.validator( mgp.lon )
    ) { (_, _) => mgp }
  }


  import play.api.libs.json._
  import play.api.libs.functional.syntax._

  val READS_GEO_ARRAY = Reads[MGeoPoint] {
    case JsArray(Seq(lonV, latV)) =>
      val latOpt = latV.asOpt[Double]
      if ( !latOpt.exists(Lat.isValid) ) {
        JsError( Lat.E_INVALID )
      } else {
        val lonOpt = lonV.asOpt[Double]
        if ( !lonOpt.exists(Lon.isValid) ) {
          JsError( Lon.E_INVALID )
        } else {
          val mgp = MGeoPoint(
            lat = latOpt.get,
            lon = lonOpt.get
          )
          MGeoPoint.validator(mgp).fold(
            errors  => JsError(errors.head),
            success => JsSuccess( success )
          )
        }
      }
    case other =>
      JsError( JsonValidationError("expected.jsarray", other) )
  }

  val WRITES_GEO_ARRAY = Writes[MGeoPoint] { gp =>
    Json.arr( gp.lon, gp.lat )
  }

  /** Десериализация из js-массива вида [-13.22,45.34]. */
  val FORMAT_GEO_ARRAY = Format[MGeoPoint](READS_GEO_ARRAY, WRITES_GEO_ARRAY)

  def objFormat(latName: String, lonName: String): OFormat[MGeoPoint] = (
    (__ \ latName).format[Double] and
    (__ \ lonName).format[Double]
  )(apply, unlift(unapply))


  object Implicits {

    /** JSON-формат для ввода-вывода в виде JSON-объекта, подлежащего рендеру в query_string
      * с полями "a" для lat и "o" для lon. */
    implicit val MGEO_POINT_FORMAT_QS_OBJECT: OFormat[MGeoPoint] = objFormat(
      latName = Lat.QS_FN,
      lonName = Lon.QS_FN
    )
  }

  implicit def univEq: UnivEq[MGeoPoint] = {
    import io.suggest.ueq.UnivEqUtil._
    UnivEq.derive
  }

}


/** Дефолтовая, пошаренная между клиентом и сервером, реализация [[IGeoPoint]]. */
case class MGeoPoint(
                      // TODO Надо обменять порядок аргументов на (lon,lat).
                      // TODO Надо это учесть в FormUtil.geoPointM, GeoPoint.FORMAT_ES_OBJECT, Implicits.MGEO_POINT_FORMAT_QS_OBJECT.
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
