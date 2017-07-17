package io.suggest.geo

import java.{lang => jl}

import org.locationtech.spatial4j.context.SpatialContext
import org.locationtech.spatial4j.io.GeohashUtils
import org.locationtech.spatial4j.shape.Point
import com.vividsolutions.jts.geom.Coordinate
import io.suggest.geo.GeoConstants.Qs
import io.suggest.model.play.qsb.QueryStringBindableImpl
import io.suggest.util.logs.MacroLogsImpl
import org.elasticsearch.common.geo.{GeoPoint => EsGeoPoint}
import play.api.libs.json._
import play.api.mvc.QueryStringBindable
import play.extras.geojson.LngLat

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 08.08.14 10:40
  * Description: Представление географической точки в рамках проекта s.io.
  * Сделано наподобии org.elasticsearch.common.geo.Geopoint с поправкой на полную неизменяемость и другие фичи.
  *
  * 2016.dec.14: Пошёл вынос класса модели в common, где уже ему было давно заготовлено место.
  * Унификация связана с попыткой использования boopickle на js-стороне.
  *
  * @see [[https://www.elastic.co/guide/en/elasticsearch/reference/current/mapping-geo-point-type.html]]
  */

object GeoPoint extends MacroLogsImpl {

  import LOGGER._

  def apply(v: String): MGeoPoint = {
    val commaIndex = getCommaIndex(v)
    if (commaIndex != -1) {
      fromLatLonComma(v, commaIndex)
    } else {
      fromGeoHash(v)
    }
  }

  def apply(esgp: EsGeoPoint): MGeoPoint = {
    MGeoPoint(lat = esgp.lat,  lon = esgp.lon)
  }

  def apply(point: Point): MGeoPoint = {
    MGeoPoint(lon = point.getX, lat = point.getY)
  }

  /** Бывает, что идёт выковыривание сырых значений из документов elasticsearch. */
  def fromArraySeq(lonLatColl: TraversableOnce[Any]): Option[MGeoPoint] = {
    lazy val logPrefix = s"fromArraySeq(${System.currentTimeMillis()}):"
    val doubleSeq = lonLatColl
      .toIterator
      .flatMap {
        case v: java.lang.Number =>
          val n = JsNumber( v.doubleValue() )
          Seq(n)
        case other =>
          warn(s"$logPrefix Unable to parse agg.value='''$other''' as Number.")
          Nil
      }
      .toSeq
    val jsRes = GeoPoint.READS_ANY
      .reads( JsArray(doubleSeq) )
    if (jsRes.isError)
      error(s"$logPrefix Agg.values parsing failed:\n $jsRes")
    jsRes.asOpt
  }

  def fromLatLon(latLon: String): MGeoPoint = {
    val commaInx = getCommaIndex(latLon)
    fromLatLonComma(latLon, commaInx)
  }

  def fromGeoHash(geoHash: String): MGeoPoint = {
    val esgp = GeohashUtils.decode(geoHash, SpatialContext.GEO)
    apply(esgp)
  }


  /** Извлечение гео-точки из различных форматов данных.
    *
    * @param r Исходный инстанс.
    * @return Опциональная точка.
    */
  def from(r: AnyRef): Option[MGeoPoint] = {
    def logPrefix = s"from($r):"
    Option(r).flatMap {
      case esgp: EsGeoPoint =>
        Some( apply(esgp) )
      case sp4jPoint: Point =>
        Some( apply(sp4jPoint) )
      case str: String =>
        try {
          Option( apply(str) )
        } catch {
          case ex: Throwable =>
            LOGGER.warn(s"$logPrefix Failed to parse geo point from string", ex)
            None
        }
      case mgp: MGeoPoint =>
        Some( mgp )
      // TraversableOnce наверное не нужен вообще, был нужен для es-2.x и ранее.
      case lonLatColl: TraversableOnce[Any] =>
        fromArraySeq( lonLatColl )
      case _ =>
        LOGGER.warn(s"$logPrefix Totally unknown geopoint format type.")
        None
    }
  }

  private def getCommaIndex(str: String) = str indexOf ','

  private def fromLatLonComma(latLon: String, commaIndex: Int): MGeoPoint = {
    val lat = jl.Double.parseDouble(latLon.substring(0, commaIndex).trim)
    val lon = jl.Double.parseDouble(latLon.substring(commaIndex + 1).trim)
    MGeoPoint(
      lat = Lat.ensureInBounds(lat),
      lon = Lon.ensureInBounds(lon)
    )
  }

  /** Десериализация из строки вида "45.34,-13.22". */
  val READS_STRING = Reads[MGeoPoint] {
    case JsString(raw) =>
      JsSuccess( apply(raw) )
    case other =>
      JsError( JsonValidationError("expected.jsstring", other) )
  }

  /** JSON-формат для ввода-вывода в виде JSON-объекта с полями lat и lon. */
  val FORMAT_ES_OBJECT: Format[MGeoPoint] = MGeoPoint.objFormat(
    latName = Lat.ES_FN,
    lonName = Lon.ES_FN
  )

  /** Десериализация из JSON из различных видов представления геоточки. */
  val READS_ANY: Reads[MGeoPoint] = {
    MGeoPoint.FORMAT_GEO_ARRAY
      .orElse( FORMAT_ES_OBJECT )
      .orElse( READS_STRING )
  }

  /** Дефолтовый JSON-форматтер для десериализации из разных форматов,
    * но сериализации в JSON object с полями lat и lon. */
  val FORMAT_ANY_TO_ARRAY = Format[MGeoPoint](READS_ANY, MGeoPoint.FORMAT_GEO_ARRAY)


  // Экспорт инстансов IGeoPoint, вынесен из класса GeoPoint перед его депортацией в [common].

  /**
   * Конвертация в GeoJson-представление координат, т.е. в JSON-массив.
   * @see [[http://www.elasticsearch.org/guide/en/elasticsearch/reference/current/mapping-geo-point-type.html#_lat_lon_as_array_5]]
   */
  def toPlayGeoJson(gp: IGeoPoint) = {
    JsArray(Seq(
      JsNumber(gp.lon), JsNumber(gp.lat)
    ))
  }

  def toEsStr(gp: IGeoPoint): String = gp.lat.toString + "," + gp.lon.toString

  /** Пространственная координата в терминах JTS. */
  def toJtsCoordinate(gp: IGeoPoint) = new Coordinate(gp.lon, gp.lat)

  /** (Lon,lat,alt) является основным порядком гео.координат в sio2. */
  def toLngLat(gp: IGeoPoint): LngLat = {
    LngLat(lng = gp.lon, lat = gp.lat)
  }

  // Предвартильно выпилено на корню в целях борьбы с зоопарком форматов координат.
  //def toLatLng(gp: IGeoPoint): LatLng = {
  //  LatLng(lat = gp.lat, lng = gp.lon)
  //}

  /** Всякие неявности изолированы в отдельном namespace. */
  object Implicits {

    implicit def GEO_POINT_FORMAT: Format[MGeoPoint] = FORMAT_ANY_TO_ARRAY

    /** Поддержка биндинга из/в Query string в play router. */
    implicit def geoPointQsb(implicit doubleB: QueryStringBindable[Double]): QueryStringBindable[MGeoPoint] = {
      new QueryStringBindableImpl[MGeoPoint] {

        override def KEY_DELIM = Qs.DELIM

        override def bind(key: String, params: Map[String, Seq[String]]): Option[Either[String, MGeoPoint]] = {
          val k = key1F(key)
          for {
            latEith <- doubleB.bind( k(Lat.QS_FN), params )
            lonEith <- doubleB.bind( k(Lon.QS_FN), params )
          } yield {
            // TODO play-2.6: scala-2.12+ syntax:
            /*latEith
              .filterOrElse( Lat.isValid, E_INVALID_LAT )
              .flatMap { lat =>
                lonEith
                  .filterOrElse( Lon.isValid, E_INVALID_LON )
                  .map { lon =>
                    MGeoPoint(
                      lat = lat,
                      lon = lon
                    )
                  }
              }*/

            // scala 2.11 syntax: TODO Заменить верхним синтаксисом после апдейта сервера до scala-2.12 (см. TODO выше).
            latEith match {
              case Right(lat) =>
                if (Lat.isValid(lat)) {
                  lonEith match {
                    case Right(lon) =>
                      if (Lon.isValid(lon)) {
                        Right(MGeoPoint(
                          lat = lat,
                          lon = lon
                        ))
                      } else {
                        Left( Lon.E_INVALID )
                      }
                    case Left(e) => Left(e)
                  }
                } else {
                  Left( Lat.E_INVALID )
                }
              case Left(e) => Left(e)
            }
          }
        }

        override def unbind(key: String, value: MGeoPoint): String = {
          _mergeUnbinded {
            val k = key1F(key)
            Seq(
              doubleB.unbind(k(Lat.QS_FN), value.lat),
              doubleB.unbind(k(Lon.QS_FN), value.lon)
            )
          }
        }
      }
    }

  }


  /** Поддержка формата "e=51.9123|33.2424".
    * Появилась для поддержки текущей точки в выдаче v2.
    */
  def pipeDelimitedQsb(implicit strB: QueryStringBindable[String]): QueryStringBindable[MGeoPoint] = {
    new QueryStringBindableImpl[MGeoPoint] {

      override def unbind(key: String, value: MGeoPoint): String = {
        strB.unbind(key, value.toString)
      }

      override def bind(key: String, params: Map[String, Seq[String]]) = {
        for {
          rawEith <- strB.bind(key, params)
        } yield {
          rawEith.right.flatMap { raw =>
            MGeoPoint.fromString(raw)
              .toRight("e.geo.point")
          }
        }
      }

    }
  }

  /** Опциональная поддержка формата "e=51.9123|33.2424"
    * Появилась для поддержки текущей точки в выдаче v2.
    */
  def pipeDelimitedQsbOpt(implicit strOptB: QueryStringBindable[Option[String]]): QueryStringBindable[Option[MGeoPoint]] = {
    new QueryStringBindableImpl[Option[MGeoPoint]] {

      override def bind(key: String, params: Map[String, Seq[String]]): Option[Either[String, Option[MGeoPoint]]] = {
        for {
          rawOptEith <- strOptB.bind(key, params)
        } yield {
          rawOptEith.right.flatMap { rawOpt =>
            rawOpt.fold[Either[String, Option[MGeoPoint]]] {
              Right(None)
            } { raw =>
              val gpOpt = MGeoPoint.fromString(raw)
              if (gpOpt.isEmpty)
                Left("e.geo.point")
              else
                Right(gpOpt)
            }
          }
        }
      }

      override def unbind(key: String, value: Option[MGeoPoint]): String = {
        strOptB.unbind(key, value.map(_.toString))
      }
    }
  }

}
