package io.suggest.adv.geo

import boopickle.Default._
import io.suggest.geo.MGeoPoint

import scalaz.{ValidationNel, Validation}
import scalaz.syntax.apply._

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
  def withZoom(zoom: Int) = copy(zoom = zoom)

}


object MMapProps {

  implicit val mmapsPickler: Pickler[MMapProps] = {
    implicit val mgpPickler = MGeoPoint.MGEO_POINT_PICKLER
    generatePickler[MMapProps]
  }

  def isZoomValid(zoom: Int): Boolean = {
    zoom > 0 && zoom < 18
  }

  def validate(mp: MMapProps): ValidationNel[String, MMapProps] = {
    (
      MGeoPoint.validator( mp.center ) |@|
      zoomValidator(mp.zoom)
    ) { (_, _) => mp }
  }

  def zoomValidator(zoom: Int): ValidationNel[String, Int] = {
    Validation.liftNel(zoom)( !isZoomValid(_), "e.zoom.invalid" )
  }

}