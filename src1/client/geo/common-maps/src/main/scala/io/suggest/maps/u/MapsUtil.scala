package io.suggest.maps.u

import io.suggest.common.geom.d2.ISize2di
import io.suggest.geo._
import io.suggest.maps.{MMapProps, MMapS}
import io.suggest.sjs.leaflet.{LatLngExpression, Leaflet, PolygonLatLngs_t}
import io.suggest.sjs.leaflet.map.{LatLng, Point}

import scala.scalajs.js.JSConverters._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 18.04.17 22:21
  * Description: JS-утиль для работы с географическими картами и геоданными.
  */
object MapsUtil {

  /** Конвертация size2di в leaflet-точку. */
  def size2d2LPoint(size2d: ISize2di): Point = {
    Leaflet.point(
      x = size2d.width,
      y = size2d.height
    )
  }


  /** Конверсия L.LatLng в MGeoPoint. */
  def latLng2geoPoint(ll: LatLng): MGeoPoint = {
    MGeoPoint.fromDouble(
      lat = ll.lat,
      lon = ll.lng
    )
  }


  /** Конверсия MGeoPoint в L.LatLng. */
  def geoPoint2LatLng(gp: MGeoPoint): LatLng = {
    Leaflet.latLng( MGeoPointJs.toLatLngArray(gp) )
  }


  /** Посчитать расстояние между двумя точками. */
  def distanceBetween(gp0: MGeoPoint, gp1: MGeoPoint): Double = {
    geoPoint2LatLng(gp0)
      .distanceTo( geoPoint2LatLng(gp1) )
  }


  object Implicits {
    implicit final class MGeoPointsExt(private val gps: IterableOnce[MGeoPoint]) extends AnyVal {

      /** Выбрать ближайшую точку к указанной точке.
        *
        * @param toPoint К какой точке искать близость.
        * @return Опциональный результат. None, когда исходный список точек пуст.
        */
      def nearestTo(toPoint: MGeoPoint): Option[MGeoPoint] = {
        val baseIter = gps.iterator
        val buffIter = baseIter.buffered
        for (first <- buffIter.headOption) yield {
          if (baseIter.isEmpty) {
            first
          } else {
            val toPointLL = geoPoint2LatLng( toPoint )
            buffIter
              .minBy { currPoint =>
                geoPoint2LatLng(currPoint) distanceTo toPointLL
              }
          }
        }
      }

    }
  }


  /**
    * Посчитать дефолтовые координаты маркера радиуса на основе указанного круга.
    * @param geoCircle Текущий круг.
    * @return Гео-точка.
    * @see [[http://gis.stackexchange.com/a/2980]]
    */
  def radiusMarkerLatLng(geoCircle: CircleGs): MGeoPoint = {
    // Считаем чисто математичеки координаты маркера радиуса. По дефолту, просто восточнее от центра на расстоянии радиуса.
    val earthRadiusM = GeoConstants.Earth.RADIUS_M

    // TODO Добавить поддержку угла. Сейчас угол всегда равен 0. Для этого нужно проецировать вектор на OX и OY через sin() и cos().
    // offsets in meters: north = +0; east = +radiusM
    // Coord.offsets in radians:
    //val dLat = 0   // пока тут у нас нет смещения на север. Поэтому просто ноль.
    val D180 = 180
    // Изображаем дробь:
    val dLon = geoCircle.radiusM / (
      earthRadiusM * Math.cos(
        Math.PI * geoCircle.center.lat.doubleValue / D180
      )
    )
    MGeoPoint.lon.modify { lon0 =>
      // OffsetPosition, decimal degrees
      lon0 + dLon * D180 / Math.PI
    }(geoCircle.center)
  }


  private def _polygon2leafletCoordsArr(polygonGs: PolygonGs) = {
    polygonGs
      .outerWithHoles
      .iterator
      .map { lsgs =>
        lsgs
          .coords
          .map( MapsUtil.geoPoint2LatLng(_): LatLngExpression )
          .toJSArray
      }
      .toJSArray
  }
  def polygon2leafletCoords(polygonGs: PolygonGs): PolygonLatLngs_t = {
    _polygon2leafletCoordsArr( polygonGs )
  }

  def multiPolygon2leafletCoords(multiPolygonGs: MultiPolygonGs): PolygonLatLngs_t = {
    multiPolygonGs
      .polygons
      .iterator
      .map( _polygon2leafletCoordsArr )
      .toJSArray
  }

  def lPolygon2leafletCoords(lPolygon: ILPolygonGs): PolygonLatLngs_t = {
    lPolygon match {
      case polygonGs: PolygonGs =>
        polygon2leafletCoords( polygonGs )
      case multiPolygonGs: MultiPolygonGs =>
        multiPolygon2leafletCoords( multiPolygonGs )
    }
  }

  /** Вычислить гео-центр этого полигона, записанного в LatLng-виде. */
  def polyLatLngs2center(positions: PolygonLatLngs_t): LatLng = {
    Leaflet.polygon( positions )
      .getBounds()
      .getCenter()
  }

  /** Сборка начального состояния MMapS.
    *
    * @param mapProps Присланный с сервера MMapProps.
    * @return Инстанс MMapS, готовый к сохранению в состояние.
    */
  def initialMapStateFrom( mapProps: MMapProps ): MMapS = {
    MMapS( mapProps )
  }

}


class DistanceUtilLeafletJs extends IDistanceUtilJs {

  override type T = LatLng

  /** Partially-implemented distance-from calculation via Leaflet.LatLng() methods. */
  sealed abstract class ILatLngDistanceFrom extends IDistanceFrom[LatLng] {
    override def distanceTo(to: IDistanceFrom[T]): Double =
      (geoPointImpl distanceTo to.geoPointImpl).toInt
  }

  /** Prepare one's point data be distance-measured.
    *
    * @return None, if not implemented/not available.
    */
  override def prepareDistanceTo(geoPoint: MGeoPoint): Some[IDistanceFrom[T]] = {
    val _geoPoint = geoPoint
    val lldf = new ILatLngDistanceFrom {
      override def geoPoint = _geoPoint
      override val geoPointImpl = MapsUtil.geoPoint2LatLng( _geoPoint )
    }
    Some( lldf )
  }

  /** Calculate center of polygon (center point of bounding rect.). */
  override def geoCenterOfPolygon(poly: ILPolygonGs): Some[IDistanceFrom[T]] = {
    val positions = MapsUtil.lPolygon2leafletCoords( poly )
    val centerLL = MapsUtil.polyLatLngs2center( positions )
    val lldf = new ILatLngDistanceFrom {
      override lazy val geoPoint = MapsUtil.latLng2geoPoint( centerLL )
      override def geoPointImpl = centerLL
    }
    Some( lldf )
  }

}
