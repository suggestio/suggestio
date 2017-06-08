package io.suggest.lk.adn.map.m

import diode.FastEq
import diode.data.Pot
import io.suggest.adv.rcvr.{MRcvrPopupResp, MRcvrPopupS}
import io.suggest.lk.adv.m.IRcvrPopupProps
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
        IRcvrPopupProps.IRcvrPopupPropsFastEq.eqv(a, b)
    }
  }

}


case class MLamRcvrs(
                      nodesResp                 : Pot[MGeoNodesResp]    = Pot.empty,
                      override val popupResp    : Pot[MRcvrPopupResp]   = Pot.empty,
                      override val popupState   : Option[MRcvrPopupS]   = None
                    )
  extends IRcvrPopupProps
{

  def withNodesResp( nodesResp: Pot[MGeoNodesResp] ) = copy( nodesResp = nodesResp )

  def withPopupResp(popupResp: Pot[MRcvrPopupResp]) = copy(popupResp = popupResp)
  def withPopupState(popupState: Option[MRcvrPopupS]) = copy(popupState = popupState)

  def withPopup(resp: Pot[MRcvrPopupResp], state: Option[MRcvrPopupS]) = copy(
    popupResp  = resp,
    popupState = state
  )

}