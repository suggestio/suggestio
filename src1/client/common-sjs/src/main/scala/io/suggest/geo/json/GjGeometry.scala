package io.suggest.geo.json

import io.suggest.msg.ErrorMsgs
import io.suggest.log.Log

import scala.scalajs.js
import scala.scalajs.js.JSON

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 13.04.16 21:36
  * Description: Модель абстрактной геометрии GeoJSON.
  */
object GjGeometry extends Log {

  def apply(gtype: String, gcoordinates: GjGeometryCoords_t): GjGeometry = {
    new GjGeometry {
      override val `type` = gtype
      override val coordinates = gcoordinates
    }
  }

  def firstPoint(geom: GjGeometry): js.Array[Double] = {
    firstPoint(geom.coordinates)
  }
  def firstPoint(coords: GjGeometryCoords_t, index: Int = 0): js.Array[Double] = {
    coords(index).asInstanceOf[Any] match {
      // Число (lon). Значит текущий массив -- это координата [x,y].
      case _: Double =>
        coords.asInstanceOf[js.Array[Double]]

      // Подмассив с координатами или другими подмассивами. Это нормально.
      case arr: js.Array[_] if arr.nonEmpty =>
        firstPoint( arr.asInstanceOf[GjGeometryCoords_t] )

      // Should never happen:
      case other =>
        logger.warn(
          ErrorMsgs.GEO_JSON_GEOM_COORD_UNEXPECTED_ELEMENT,
          msg = JSON.stringify(coords) + " " + other
        )
        firstPoint(coords, index + 1)
    }
  }

}


trait GjGeometry extends GjType {

  val coordinates: GjGeometryCoords_t

}
