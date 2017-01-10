package io.suggest.adv.geo

import boopickle.Default._
import io.suggest.geo.MGeoPoint

/** Интерфейс модели состояния карты.
  *
  * @param center Текущий центр карты.
  * @param zoom Текущий зум карты.
  */
case class MMapProps(
  center  : MGeoPoint,
  zoom    : Int
) {

  def withCenter(center2: MGeoPoint) = copy(center = center2)

}


object MMapProps {

  implicit val pickler: Pickler[MMapProps] = {
    implicit val mgpPickler = MGeoPoint.pickler
    generatePickler[MMapProps]
  }

}