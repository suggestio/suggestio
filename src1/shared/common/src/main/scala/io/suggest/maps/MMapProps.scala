package io.suggest.maps

import boopickle.Default._
import io.suggest.geo.MGeoPoint
import io.suggest.geo.MGeoPoint.Implicits.MGEO_POINT_FORMAT_QS_OBJECT
import play.api.libs.json._
import play.api.libs.functional.syntax._

import scalaz.syntax.apply._
import scalaz.{Validation, ValidationNel}

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

  /** Поддержка play-json. */
  implicit val MMAP_PROPS_FORMAT: OFormat[MMapProps] = (
    (__ \ "c").format[MGeoPoint] and
    (__ \ "z").format[Int]
  )(apply, unlift(unapply))


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