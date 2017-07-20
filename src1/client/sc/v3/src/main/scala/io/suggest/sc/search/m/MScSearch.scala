package io.suggest.sc.search.m

import diode.FastEq
import diode.data.Pot
import io.suggest.maps.m.MMapS
import io.suggest.maps.nodes.MGeoNodesResp

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 20.07.17 14:31
  * Description: Над-модель для панели поиска.
  */
object MScSearch {

  implicit object MScSearchFastEq extends FastEq[MScSearch] {
    override def eqv(a: MScSearch, b: MScSearch): Boolean = {
      (a.mapState eq b.mapState) &&
        (a.text eq b.text) &&
        (a.rcvrsGeo eq b.rcvrsGeo)
    }
  }

}


case class MScSearch(
                      mapState    : MMapS,
                      text        : Option[MScSearchText] = None,
                      rcvrsGeo    : Pot[MGeoNodesResp] = Pot.empty
                    ) {

  def withMapState( mapState: MMapS ) = copy(mapState = mapState)
  def withText( text: Option[MScSearchText] ) = copy(text = text)
  def withRcvrsGeo( rcvrsGeo: Pot[MGeoNodesResp] ) = copy( rcvrsGeo = rcvrsGeo )

}
