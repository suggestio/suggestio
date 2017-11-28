package io.suggest.maps.m

import diode.FastEq
import io.suggest.geo.MGeoPoint
import io.suggest.maps.MMapProps
import japgolly.univeq._
import io.suggest.ueq.UnivEqUtil._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 10.01.17 18:55
  * Description: Клиентская модель данных состояния простейшей карты.
  * Изначально под такой картой подразумевалась leaflet-карта с кнопкой "где я?",
  * возможностью перемещения и масштабирования этой карты.
  */
object MMapS {

  implicit object MMapSFastEq extends FastEq[MMapS] {
    override def eqv(a: MMapS, b: MMapS): Boolean = {
      (a.zoom ==* b.zoom) &&
        (a.centerInit ===* b.centerInit) &&
        // Не реагировать на изменение реальной координаты, т.к. сюда идёт запись только из callback'ов карты.
        //(a.centerReal ===* b.centerReal) &&
        (a.locationFound ===* b.locationFound)
    }
  }

  implicit def univEq: UnivEq[MMapS] = UnivEq.derive

  def apply(mapProps: MMapProps): MMapS = {
    MMapS(
      zoom        = mapProps.zoom,
      centerInit  = mapProps.center
    )
  }

}


/**
  * Класс клиентской модели данных по карте.
  * @param locationFound Состояние геолокации и реакции на неё:
  *                      true уже карта была отцентрована по обнаруженной геолокации.
  *                      false началась геолокация, нужно отцентровать карту по опредённым координатам.
  *                      None Нет ни геолокации, ничего.
  * @param centerReal Фактический центр карты после перемещения.
  *                   Запись напрямую в MMapProps приводит к плохому поведению leaflet,
  *                   поэтому фактический центр сохраняется отдельно от центра инициализации.
  */
case class MMapS(
                  zoom          : Int,
                  centerInit    : MGeoPoint,
                  centerReal    : Option[MGeoPoint]   = None,
                  locationFound : Option[Boolean]     = None,
                ) {

  def withZoom(zoom: Int) = copy(zoom = zoom)
  def withCenterInit(centerInit: MGeoPoint) = copy(centerInit = centerInit)
  def withCenterReal(centerReal: Option[MGeoPoint]) = copy(centerReal = centerReal)
  def withLocationFound(lf: Option[Boolean]) = copy(locationFound = lf)

  def center: MGeoPoint = {
    centerReal
      .getOrElse(centerInit)
  }

  /** Кросс-платформенные данные карты, для экспорта на сервер. */
  def toMapProps: MMapProps = {
    MMapProps(
      center = center,
      zoom   = zoom
    )
  }

}
