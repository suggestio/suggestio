package io.suggest.maps.m

import io.suggest.geo.MGeoPoint
import japgolly.univeq.UnivEq
import monocle.macros.GenLens

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 10.01.17 13:32
  * Description: Рантаймовое состояние rad-компонентов.
  *
  * @param radiusMarkerCoords Координаты маркера управления радиусом.
  * @param centerDragging Сейчас происходит перетаскивание центра. Здесь лежит текущая координата центра.
  * @param radiusDragging Сейчас происходит такскание радиуса.
  */
case class MRadS(
                  radiusMarkerCoords  : MGeoPoint,
                  centerDragging      : Option[MGeoPoint] = None,
                  radiusDragging      : Boolean           = false
                )


object MRadS {

  @inline implicit def univEq: UnivEq[MRadS] = UnivEq.derive

  def radiusMarkerCoords = GenLens[MRadS](_.radiusMarkerCoords)
  def centerDragging = GenLens[MRadS](_.centerDragging)
  def radiusDragging = GenLens[MRadS](_.radiusDragging)

}
