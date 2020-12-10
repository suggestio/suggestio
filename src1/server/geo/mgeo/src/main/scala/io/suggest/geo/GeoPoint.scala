package io.suggest.geo

import org.locationtech.spatial4j.context.SpatialContext
import org.locationtech.spatial4j.io.GeohashUtils
import org.locationtech.spatial4j.shape.Point
import com.vividsolutions.jts.geom.Coordinate
import io.suggest.geo.GeoConstants.Qs
import io.suggest.util.logs.MacroLogsImpl
import org.elasticsearch.common.geo.{GeoPoint => EsGeoPoint}
import play.api.libs.json._
import play.api.mvc.QueryStringBindable
import au.id.jazzy.play.geojson.LngLat
import io.suggest.xplay.qsb.QueryStringBindableImpl

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 08.08.14 10:40
  * Description: Представление географической точки в рамках проекта s.io.
  * Сделано наподобии org.elasticsearch.common.geo.Geopoint с поправкой на полную неизменяемость и другие фичи.
  *
  * @see [[https://www.elastic.co/guide/en/elasticsearch/reference/current/mapping-geo-point-type.html]]
  */

object GeoPoint extends MacroLogsImpl {

  def apply(v: String): MGeoPoint =
    MGeoPoint.fromLatLonComma(v)

  def apply(esgp: EsGeoPoint): MGeoPoint =
    MGeoPoint(lat = esgp.lat,  lon = esgp.lon)

  def apply(point: Point): MGeoPoint =
    MGeoPoint(lon = point.getX, lat = point.getY)


  def fromGeoHash(geoHash: String): MGeoPoint = {
    val esgp = GeohashUtils.decode(geoHash, SpatialContext.GEO)
    apply(esgp)
  }


  /** Пространственная координата в терминах JTS. */
  def toJtsCoordinate(gp: MGeoPoint) = new Coordinate(gp.lon.doubleValue, gp.lat.doubleValue)

  /** (Lon,lat,alt) является основным порядком гео.координат в sio2. */
  def toLngLat(gp: MGeoPoint): LngLat = {
    LngLat(
      lng = gp.lon.doubleValue,
      lat = gp.lat.doubleValue
    )
  }


  /** Всякие неявности изолированы в отдельном namespace. */
  object Implicits {

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
            // play-2.6: scala-2.12+ syntax:
            latEith
              .filterOrElse( Lat.isValid(_), Lat.E_INVALID )
              .flatMap { lat =>
                lonEith
                  .filterOrElse( Lon.isValid(_), Lon.E_INVALID )
                  .map { lon =>
                    MGeoPoint(
                      lat = lat,
                      lon = lon
                    )
                  }
              }
          }
        }

        override def unbind(key: String, value: MGeoPoint): String = {
          _mergeUnbinded {
            val k = key1F(key)
            Seq(
              doubleB.unbind(k(Lat.QS_FN), value.lat.doubleValue),
              doubleB.unbind(k(Lon.QS_FN), value.lon.doubleValue)
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
          rawEith.flatMap { raw =>
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
          rawOptEith.flatMap { rawOpt =>
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
