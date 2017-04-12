package io.suggest.lk.adv.geo.m

import diode.FastEq
import io.suggest.lk.adv.m.MPriceS

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 06.04.17 22:32
  * Description: Состояния данных, связанных с биллингом.
  */
object MBillS {

  /** Быстрый компаратор FastEq. */
  implicit object MBillSFastEq extends FastEq[MBillS] {
    override def eqv(a: MBillS, b: MBillS): Boolean = {
      a.price eq b.price
    }
  }

}


/**
  * Состояние данных по биллингу.
  *
  * @param price Состояние ценника.
  */
case class MBillS(
                   price         : MPriceS                  = MPriceS()
                 ) {

  def withPrice(price2: MPriceS) = copy(price = price2)

}
