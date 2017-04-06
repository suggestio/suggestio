package io.suggest.lk.adv.geo.m

import diode.FastEq
import diode.data.Pot
import io.suggest.bill.MDetailedPriceResp

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 06.04.17 22:34
  * Description: Состояния детализованной инфы по элементу формирования стоимости.
  */
object MBillDetailedS {
  implicit object MBillDatailedSFastEq extends FastEq[MBillDetailedS] {
    override def eqv(a: MBillDetailedS, b: MBillDetailedS): Boolean = {
      (a.detailed eq b.detailed) &&
        (a.index == b.index)
    }
  }
}


case class MBillDetailedS(
                           index       : Int,
                           detailed    : Pot[MDetailedPriceResp]
                         ) {

  def withDetailed(detailed2: Pot[MDetailedPriceResp]) = copy( detailed = detailed2 )

}
