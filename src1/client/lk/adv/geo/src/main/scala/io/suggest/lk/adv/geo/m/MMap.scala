package io.suggest.lk.adv.geo.m

import diode.FastEq
import io.suggest.adv.geo.MMapProps

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 10.01.17 18:55
  * Description: Клиентская модель данных состояния карты.
  */
object MMap {

  implicit object MMapFastEq extends FastEq[MMap] {
    override def eqv(a: MMap, b: MMap): Boolean = {
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
case class MMap(
  props         : MMapProps,
  locationFound : Option[Boolean] = None
) {

  def withProps(mmp: MMapProps) = copy(props = mmp)

  def withLocationFound(lf: Option[Boolean]) = copy(locationFound = lf)

}
