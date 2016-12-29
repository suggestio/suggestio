package io.suggest.lk.adv.geo.m

import diode.FastEq
import diode.data.Pot
import io.suggest.adv.geo.MGeoAdvExistPopupResp
import io.suggest.sjs.common.geo.json.GjFeature

import scala.scalajs.js

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 28.12.16 21:50
  * Description: Корневая модель данных по каким-то текущим размещениям.
  */
object MGeoAdvs {

  /** Поддержка diode FastEq для инстансов [[MGeoAdvs]]. */
  implicit object MGeoAdvsFastEq extends FastEq[MGeoAdvs] {
    override def eqv(a: MGeoAdvs, b: MGeoAdvs): Boolean = {
      (a.existResp eq b.existResp) &&
        (a.popupResp eq b.popupResp)
    }
  }

}

case class MGeoAdvs(
                     existResp   : Pot[js.Array[GjFeature]]     = Pot.empty,
                     popupResp   : Pot[MGeoAdvExistPopupResp]   = Pot.empty,
                     popupState  : Option[MGeoAdvPopupState]    = None
                   ) {

  def withExistResp(resp2: Pot[js.Array[GjFeature]])      = copy(existResp = resp2)
  def withPopupResp(resp2: Pot[MGeoAdvExistPopupResp])    = copy(popupResp = resp2)
  def withPopupState(state2: Option[MGeoAdvPopupState])   = copy(popupState = state2)
}
