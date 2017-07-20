package io.suggest.maps.m

import diode.FastEq
import io.suggest.maps.MMapProps

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
      (a.props eq b.props) &&
        (a.locationFound eq b.locationFound)
    }
  }

}


/**
  * Класс клиентской модели данных по карте.
  * @param props Кросс-платформенные данные карты.
  * @param locationFound Состояние геолокации и реакции на неё:
  *                      true уже карта была отцентрована по обнаруженной геолокации.
  *                      false началась геолокация, нужно отцентровать карту по опредённым координатам.
  *                      None Нет ни геолокации, ничего.
  */
case class MMapS(
                  props         : MMapProps,
                  locationFound : Option[Boolean] = None
                ) {

  def withProps(mmp: MMapProps) = copy(props = mmp)
  def withLocationFound(lf: Option[Boolean]) = copy(locationFound = lf)

}
