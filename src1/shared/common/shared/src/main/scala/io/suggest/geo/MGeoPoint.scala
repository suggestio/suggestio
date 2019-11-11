package io.suggest.geo

import boopickle.Default._
import diode.FastEq
import io.suggest.common.geom.coord.{GeoCoord_t, ICoord2dHelper}
import io.suggest.common.html.HtmlConstants.{SPACE, `(`, `)`}
import io.suggest.geo.GeoConstants.Qs
import io.suggest.math.MathConst
import japgolly.univeq.UnivEq
import scalaz.ValidationNel
import scalaz.syntax.apply._
import io.suggest.ueq.UnivEqUtil._

import scala.util.Try

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 20.05.15 15:44
 * Description: Модель гео-точки.
 */

object MGeoPoint {

  implicit val MGEO_POINT_PICKLER: Pickler[MGeoPoint] = generatePickler[MGeoPoint]

  /** Константы для описания точности точек.
    * osm.org использует точность в зависимости от масштаба, но не более 5 знаков после запятой.
    */
  object FracScale {
    /** Обычная (высокая) точность. */
    def DEFAULT = 5
    /** Точность условно-близких точек. */
    def NEAR = DEFAULT - 1
  }


  /** Сборка инстанс [[MGeoPoint]] на основе обычных double-значений координат.
    *
    * У double проблема, что координата вида 50.345 может принимать вид 50.34499999693999401,
    * что вызывает кучу проблем с кэшированием, округлением, хранением, передачей и проч.
    *
    * Поэтому, 2018-12-11 всё заполыхало окончательно, и был выполнен переход на BigDecimal.
    * А тут метод для совместимости, вместо apply(), который автоматом нормализует представление.
    *
    * @return Инстанс MGeoPoint с нормализованными координатами.
    */
  def fromDouble(lat: Double, lon: Double): MGeoPoint = {
    apply(lat = lat, lon = lon)
      .withCoordScale( FracScale.DEFAULT )
  }

  def unapplyDouble(mgp: MGeoPoint) = {
    for ((a, b) <- unapply(mgp))
    yield (a.doubleValue, b.doubleValue)
  }

  /** Проверить точку на валидность координат. */
  def isValid(gp: MGeoPoint): Boolean = {
    Lat.isValid(gp.lat) &&
    Lon.isValid(gp.lon)
  }


  def fromString(str: String): Option[MGeoPoint] = {
    str.split(Qs.LAT_LON_DELIM_FN) match {
      case Array(latStr, lonStr) =>
        Try(
          MGeoPoint.fromDouble(
            lat = latStr.toDouble,
            lon = lonStr.toDouble
          )
        )
          .toOption

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
    case JsArray( collection.Seq(lonV, latV) ) =>
      val latOpt = latV.asOpt[GeoCoord_t]
      if ( !latOpt.exists(Lat.isValid) ) {
        JsError( Lat.E_INVALID )
      } else {
        val lonOpt = lonV.asOpt[GeoCoord_t]
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


  /** Рендер в JsObject. */
  def objFormat(latName: String, lonName: String): OFormat[MGeoPoint] = {
    val formatter = implicitly[Format[GeoCoord_t]]
    (
      (__ \ latName).format(formatter) and
      (__ \ lonName).format(formatter)
    )(apply, unlift(unapply))
  }


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

  @inline implicit def univEq: UnivEq[MGeoPoint] = {
    import io.suggest.ueq.UnivEqUtil._
    UnivEq.derive
  }


  implicit class MGeoPointOps(val that: MGeoPoint) extends AnyVal {

    /** Примерно равная точка -- это находящаяся ну очень близко. */
    def ~=(other: MGeoPoint): Boolean = {
      val maxDiff: GeoCoord_t = Math.pow(10, -FracScale.DEFAULT)
      (that.lat - other.lat).abs < maxDiff &&
      (that.lon - other.lon).abs < maxDiff
    }

  }


  /** Примеры гео-точек. */
  object Examples {

    /** Центр СПб, адмиралтейство / штаб ВМФ. */
    def RU_SPB_CENTER = MGeoPoint(lat = 59.93769, lon = 30.30887)

  }


  object GeoPointsNearbyFastEq extends FastEq[MGeoPoint] {
    override def eqv(a: MGeoPoint, b: MGeoPoint): Boolean = {
      a ~= b
    }
  }

  /** Адаптер-typeclass для поддержки статических операций в CoordOps. */
  implicit object GeoPointCoord2dHelper extends ICoord2dHelper[MGeoPoint, GeoCoord_t] {
    // lat -> Y, lon -> X согласно требованиям математики.
    override def getX(mgp: MGeoPoint): GeoCoord_t = mgp.lon
    override def getY(mgp: MGeoPoint): GeoCoord_t = mgp.lat
  }

}


/** Дефолтовая, пошаренная между клиентом и сервером. */
case class MGeoPoint(
                      // TODO Надо обменять порядок аргументов на (lon,lat).
                      // TODO Надо это учесть в FormUtil.geoPointM, GeoPoint.FORMAT_ES_OBJECT, Implicits.MGEO_POINT_FORMAT_QS_OBJECT, в Sc3Router
                      lat: GeoCoord_t,
                      lon: GeoCoord_t,
                    ) {

  def withLat(lat2: GeoCoord_t) = copy(lat = lat2)
  def withLon(lon2: GeoCoord_t) = copy(lon = lon2)

  // TODO заменить на "lon|lat" ? Пользователю в браузере конечно удобенее "lat|lon", надо поразмыслить над этим.
  override def toString: String =
    lat.toString + Qs.LAT_LON_DELIM_FN + lon.toString


  /** (12.1234 65.5633) */
  def toHumanFriendlyString: String = {
    def _fmt(coord: GeoCoord_t) =
      "%1.2f".format(coord.toFloat)     // Напрямую format(BigDecimal) нельзя из-за странной ошибки.
    `(` + _fmt(lat) + SPACE + _fmt(lon) + `)`
  }


  def withCoordScale(scale: Int): MGeoPoint = {
    copy(
      lat = MathConst.FracScale.scaledOptimal(lat, scale),
      lon = MathConst.FracScale.scaledOptimal(lon, scale),
    )
  }

}


/** Интерфейс для моделей с полем geoPoint. */
trait IGeoPointField {
  def geoPoint: MGeoPoint
}
