package io.suggest.lk.adv.geo.m

import io.suggest.geo.MGeoPoint

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 10.01.17 13:32
  * Description: Рантаймовое состояние rad-компонентов.
  *
  * @param radiusMarkerCoords Координаты маркера управления радиусом.
  * @param centerDragging Сейчас происходит перетаскивание центра.
  * @param radiusDragging Сейчас происходит такскание радиуса.
  */
case class MRadS(
  radiusMarkerCoords  : MGeoPoint,
  centerDragging      : Boolean = false,
  radiusDragging      : Boolean = false
) {

  def withCenterDragging(cd2: Boolean) = copy(centerDragging = cd2)
  def withRadiusDragging(rd2: Boolean) = copy(radiusDragging = rd2)

}
