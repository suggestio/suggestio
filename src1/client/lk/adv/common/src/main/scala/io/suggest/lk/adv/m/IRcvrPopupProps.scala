package io.suggest.lk.adv.m

import diode.FastEq
import diode.data.Pot
import io.suggest.adv.rcvr.{MRcvrPopupResp, MRcvrPopupS}

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 08.06.17 18:55
  * Description: Интерфейс для моделей, описывающих попап над rcvr-узлом на гео.карте.
  */

object IRcvrPopupProps {

  implicit object IRcvrPopupPropsFastEq extends FastEq[IRcvrPopupProps] {
    override def eqv(a: IRcvrPopupProps, b: IRcvrPopupProps): Boolean = {
      (a.popupResp eq b.popupResp) &&
        (a.popupState eq b.popupState)
    }
  }

}

trait IRcvrPopupProps {

  def popupResp   : Pot[MRcvrPopupResp]

  def popupState  : Option[MRcvrPopupS]

  def withPopupResp(popupResp: Pot[MRcvrPopupResp]): IRcvrPopupProps
  def withPopupState(popupState: Option[MRcvrPopupS]): IRcvrPopupProps
  def withPopup(resp: Pot[MRcvrPopupResp], state: Option[MRcvrPopupS]): IRcvrPopupProps

}

