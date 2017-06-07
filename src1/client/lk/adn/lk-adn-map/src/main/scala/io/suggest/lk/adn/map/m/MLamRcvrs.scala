package io.suggest.lk.adn.map.m

import diode.FastEq
import diode.data.Pot
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
      a.nodesResp eq b.nodesResp
    }
  }

}


case class MLamRcvrs(
                      nodesResp   : Pot[MGeoNodesResp]  = Pot.empty
                    ) {

  def withNodesResp( nodesResp: Pot[MGeoNodesResp] ) = copy( nodesResp = nodesResp )

}