package io.suggest.sc.sjs.m.mmap

import io.suggest.sc.map.ScMapConstants.Mqs._
import io.suggest.sjs.mapbox.gl.Zoom_t
import io.suggest.sjs.mapbox.gl.ll.LngLatBounds

import scala.scalajs.js.{Dictionary, Any}

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 20.04.16 9:55
  * Description: Модель аргументав запроса GeoJSON'а узлов.
  */

case class MapNodesReqArgs(
  llb   : LngLatBounds,
  zoom  : Zoom_t
) {

  /** Сериализация в JSON. */
  def toJson: Dictionary[Any] = {
    val nw = llb.getNorthWest()
    val se = llb.getSouthEast()
    Dictionary[Any](
      Full.ENVELOPE_TOP_LEFT_LAT      -> nw.lat,
      Full.ENVELOPE_TOP_LEFT_LON      -> nw.lng,
      Full.ENVELOPE_BOTTOM_RIGHT_LAT  -> se.lat,
      Full.ENVELOPE_BOTTOM_RIGHT_LON  -> se.lng,
      ZOOM_FN                         -> zoom
    )
  }

}
