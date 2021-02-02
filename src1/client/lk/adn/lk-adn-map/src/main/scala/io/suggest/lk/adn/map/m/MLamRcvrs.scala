package io.suggest.lk.adn.map.m

import diode.FastEq
import diode.data.Pot
import io.suggest.adv.info.MNodeAdvInfo
import io.suggest.maps.m.MRcvrPopupS
import io.suggest.maps.nodes.MGeoNodesResp

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 07.06.17 19:00
  * Description: Модель состояния других (посторонних) ресиверов в LAM-форме.
  */
object MLamRcvrs {

  implicit object MLamRcvrsFastEq extends FastEq[MLamRcvrs] {
    override def eqv(a: MLamRcvrs, b: MLamRcvrs): Boolean = {
      (a.nodesResp eq b.nodesResp) &&
        (a.popupResp eq b.popupResp) &&
        (a.popupState eq b.popupState)
    }
  }

}


case class MLamRcvrs(
                      nodesResp    : Pot[MGeoNodesResp]    = Pot.empty,
                      popupResp    : Pot[MNodeAdvInfo]     = Pot.empty,
                      popupState   : Option[MRcvrPopupS]   = None
                    )
{

  def withNodesResp( nodesResp: Pot[MGeoNodesResp] ) = copy( nodesResp = nodesResp )

  def withPopupResp(popupResp: Pot[MNodeAdvInfo]) = copy(popupResp = popupResp)
  def withPopupState(popupState: Option[MRcvrPopupS]) = copy(popupState = popupState)

  def withPopup(resp: Pot[MNodeAdvInfo], state: Option[MRcvrPopupS]) = copy(
    popupResp  = resp,
    popupState = state
  )

}