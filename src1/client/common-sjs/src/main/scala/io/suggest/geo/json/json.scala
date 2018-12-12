package io.suggest.geo

import scala.scalajs.js
import scala.scalajs.js.|

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 12.12.18 13:53
  */
package object json {

  /** Тип одной координаты GeoJSON. */
  type GjCoord_t = js.Array[Double]

  // Два эквивалентных типа, но scala их различает.
  /** Основной тип координт геометрии GeoJSON. Включает в себя все допустимые типы массивов координат. */
  type GjGeometryCoords_t = GjCoord_t | js.Array[GjCoord_t] | js.Array[js.Array[GjCoord_t]] | js.Array[js.Array[js.Array[GjCoord_t]]]

  /** Тип, эквивалентный  [[GjGeometryCoords_t]] (но scala этого не понимает),
    * но верхний массив вынесен за скобки для нужд некоторых методов. */
  type GjGeometryCoordsArr_t = js.Array[Double | GjCoord_t | js.Array[GjCoord_t] | js.Array[js.Array[GjCoord_t]]]


  implicit def gjGeomToArr(src: GjGeometryCoords_t): GjGeometryCoordsArr_t = {
    src.asInstanceOf[GjGeometryCoordsArr_t]
  }

  implicit def gjGeomFromArr(src: GjGeometryCoordsArr_t): GjGeometryCoords_t = {
    src.asInstanceOf[GjGeometryCoords_t]
  }

}
