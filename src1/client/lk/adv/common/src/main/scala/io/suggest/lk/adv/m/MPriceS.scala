package io.suggest.lk.adv.m

import diode.FastEq
import diode.data.Pot
import io.suggest.bill.MGetPriceResp

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 16.01.17 11:27
  * Description: Модель состояния цены и состояния её рассчёта.
  */
object MPriceS {

  implicit object MPriceSFastEq extends FastEq[MPriceS] {
    override def eqv(a: MPriceS, b: MPriceS): Boolean = {
      a.resp eq b.resp
    }
  }

}

case class MPriceS(
                    resp     : Pot[MGetPriceResp]  = Pot.empty
                  )
{
  def withPriceResp(pr2: Pot[MGetPriceResp]) = copy(resp = pr2)
}