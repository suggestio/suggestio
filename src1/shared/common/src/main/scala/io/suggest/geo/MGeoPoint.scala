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
 * Description: Модель гео-точки.
 */

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


  /** JSON-формат для ввода-вывода в виде JSON-объекта с полями lat и lon. */
  def FORMAT_ES_OBJECT: Format[MGeoPoint] = MGeoPoint.objFormat(
    latName = Lat.ES_FN,
    lonName = Lon.ES_FN
  )


  object JsonFormatters {

    /** JSON-формат для ввода-вывода в виде JSON-объекта, подлежащего рендеру в query_string
      * с полями "a" для lat и "o" для lon. */
    implicit val QS_OBJECT: OFormat[MGeoPoint] = objFormat(
      latName = Lat.QS_FN,
      lonName = Lon.QS_FN
    )

    implicit val ARRAY_OR_ES_OBJECT: Format[MGeoPoint] = {
      val fmt0 = FORMAT_GEO_ARRAY
      val fallbackReads = fmt0.orElse { FORMAT_ES_OBJECT }
      Format(fallbackReads, fmt0)
    }

    /** JSON-десериализация из строки вида "54.123456|66.123456" */
    implicit def PIPE_DELIM_STRING_READS: Reads[MGeoPoint] = {
      Reads[MGeoPoint] {
        case jsStr: JsString =>
          fromString(jsStr.value)
            .fold [JsResult[MGeoPoint]] (JsError(jsStr.value)) (JsSuccess(_))
        case other =>
          JsError( other.toString() )
      }
    }

    /** JSON-сериализация в строку вида "54.123456|66.123456" */
    implicit def PIPE_DELIM_STRING_WRITES: Writes[MGeoPoint] = {
      Writes[MGeoPoint] { mgp =>
        JsString( mgp.toString )
      }
    }

  }

  implicit def univEq: UnivEq[MGeoPoint] = {
    import io.suggest.ueq.UnivEqUtil._
    UnivEq.derive
  }


  implicit class MGeoPointOps(val that: MGeoPoint) extends AnyVal {

    /** Примерно равная точка -- это находящаяся ну очень близко. */
    def ~=(other: MGeoPoint): Boolean = {
      val maxDiff = 0.000005
      Math.abs(that.lat - other.lat) < maxDiff &&
        Math.abs(that.lon - other.lon) < maxDiff
    }

  }

}


/** Дефолтовая, пошаренная между клиентом и сервером. */
case class MGeoPoint(
                      // TODO Надо обменять порядок аргументов на (lon,lat).
                      // TODO Надо это учесть в FormUtil.geoPointM, GeoPoint.FORMAT_ES_OBJECT, Implicits.MGEO_POINT_FORMAT_QS_OBJECT, в Sc3Router
                      lat: Double,
                      lon: Double
                    ) {

  def withLat(lat2: Double) = copy(lat = lat2)
  def withLon(lon2: Double) = copy(lon = lon2)

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


/** Интерфейс для моделей с полем geoPoint. */
trait IGeoPointField {
  def geoPoint: MGeoPoint
}
