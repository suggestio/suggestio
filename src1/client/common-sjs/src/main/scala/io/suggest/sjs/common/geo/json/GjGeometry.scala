package io.suggest.sjs.common.geo.json

import io.suggest.sjs.common.log.Log
import io.suggest.sjs.common.msg.WarnMsgs

import scala.scalajs.js
import scala.scalajs.js.JSON
import scala.scalajs.js.annotation.ScalaJSDefined

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 13.04.16 21:36
  * Description: Модель абстрактной геометрии GeoJSON.
  */
object GjGeometry extends Log {

  def apply(gtype: String, gcoordinates: js.Array[js.Any]): GjGeometry = {
    new GjGeometry {
      override val `type` = gtype
      override val coordinates = gcoordinates
    }
  }

  def firstPoint(geom: GjGeometry): js.Array[Double] = {
    firstPoint(geom.coordinates)
  }
  def firstPoint(coords: js.Array[_], index: Int = 0): js.Array[Double] = {
    coords(index) match {
      // Число (lon). Значит текущий массив -- это координата [x,y].
      case _: Double =>
        coords.asInstanceOf[js.Array[Double]]

      // Подмассив с координатами или другими подмассивами. Это нормально.
      case arr: js.Array[_] if arr.nonEmpty =>
        firstPoint(arr)

      // Should never happen:
      case other =>
        LOG.warn(
          WarnMsgs.GEO_JSON_GEOM_COORD_UNEXPECTED_ELEMENT,
          msg = JSON.stringify(coords) + " " + other
        )
        firstPoint(coords, index + 1)
    }
  }

}


@ScalaJSDefined
trait GjGeometry extends GjType {

  val coordinates: js.Array[js.Any]

}
