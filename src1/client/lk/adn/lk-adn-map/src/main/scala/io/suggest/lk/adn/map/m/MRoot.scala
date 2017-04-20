package io.suggest.lk.adn.map.m

import diode.FastEq
import io.suggest.adv.free.MAdv4Free
import io.suggest.dt.MAdvPeriod
import io.suggest.lk.adv.m.MPriceS
import io.suggest.maps.m.MMapS

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 18.04.17 21:32
  * Description: Корневая модель состояния компонента fsm-mvm.
  */
object MRoot {

  /** Поддержка FastEq. */
  implicit object MRootFastEq extends FastEq[MRoot] {
    override def eqv(a: MRoot, b: MRoot): Boolean = {
      (a.map eq b.map) &&
        (a.nodeMarker eq b.nodeMarker) &&
        (a.adv4free eq b.adv4free) &&
        (a.price eq b.price)
    }
  }

}


/** Класс корневой модели состояния формы.
  *
  * @param map Состояние географической карты.
  * @param nodeMarker Состояния размещения.
  */
case class MRoot(
                  map           : MMapS,
                  nodeMarker    : MNodeMarkerS,
                  adv4free      : Option[MAdv4Free],
                  price         : MPriceS,
                  datePeriod    : MAdvPeriod
                ) {

  def withMap(map2: MMapS) = copy(map = map2)
  def withNodeMarker(nm2: MNodeMarkerS) = copy(nodeMarker = nm2)
  def withAdv4Free(a4fOpt: Option[MAdv4Free]) = copy(adv4free = a4fOpt)
  def withPrice(price2: MPriceS) = copy(price = price2)
  def withDatePeriod(dp2: MAdvPeriod) = copy(datePeriod = dp2)

}
