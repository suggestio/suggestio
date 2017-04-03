package io.suggest.lk.adv.geo.m

import diode.FastEq
import diode.data.Pot
import io.suggest.adv.rcvr.RcvrKey
import io.suggest.sjs.common.vm.spa.IMPot

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 31.03.17 16:16
  * Description: Модель состояния попапа с инфой по узлу.
  */
object MNodeInfoPopupS {

  implicit object MNodeInfoPopupFastEq extends FastEq[MNodeInfoPopupS] {
    override def eqv(a: MNodeInfoPopupS, b: MNodeInfoPopupS): Boolean = {
      (a.rcvrKey eq b.rcvrKey) &&
        (a.req eq b.req)
    }
  }

}


/** Класс модели инфы по попапу с инфой по узлу.
  *
  * @param req Потенциальные данные по реквесту.
  */
case class MNodeInfoPopupS(
                            rcvrKey : RcvrKey,
                            req     : Pot[String]
                          )
  extends IMPot[String]
{

  def withReq(req2: Pot[String]) = copy(req = req2)

  override final def _pot = req

}
