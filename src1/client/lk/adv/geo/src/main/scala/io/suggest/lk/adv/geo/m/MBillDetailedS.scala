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
      (a.req eq b.req) &&
        (a.itemIndex == b.itemIndex)
    }
  }
}


/** Класс состояния детализованных данных биллинга по какому-то элементу.
  *
  * @param itemIndex индекс детализуемого элемента.
  * @param ts timestamp запроса.
  * @param req Потенциальное состояние ответа сервера по вопросу детализации.
  */
case class MBillDetailedS(
                           itemIndex   : Int,
                           ts          : Long,
                           req    : Pot[MDetailedPriceResp]
                         ) {

  def withReq(req2: Pot[MDetailedPriceResp]) = copy( req = req2 )

}
