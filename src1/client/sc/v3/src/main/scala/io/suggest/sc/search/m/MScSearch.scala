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

  /** Поддержка FastEq для инстансов [[MScSearch]]. */
  implicit object MScSearchFastEq extends FastEq[MScSearch] {
    override def eqv(a: MScSearch, b: MScSearch): Boolean = {
      (a.mapState eq b.mapState) &&
        (a.text eq b.text) &&
        (a.rcvrsGeo eq b.rcvrsGeo) &&
        (a.currTab eq b.currTab) &&
        (a.isShown == b.isShown)
    }
  }

}


/** Класс состояния панели поиска.
  *
  * @param mapState Состояние гео-карты.
  * @param text Состояние текстового поиска.
  * @param rcvrsGeo Гео-данные ресиверов.
  * @param currTab Текущий таб на панели поиска.
  * @param isShown Открыта ли панель поиска на экране?
  * @param isMapInitialized Используется ленивая инициализация карты, т.е. пока не открыт таб
  *                 в первый раз -- нет инициализации карты. В т.ч. из-за проблемы [[https://stackoverflow.com/a/36257493]]
  */
case class MScSearch(
                      mapState    : MMapS,
                      text        : Option[MScSearchText] = None,
                      rcvrsGeo    : Pot[MGeoNodesResp]    = Pot.empty,
                      currTab     : MSearchTab            = MSearchTabs.default,
                      isShown     : Boolean               = false,
                      isMapInitialized    : Boolean               = false
                    ) {

  def withMapState( mapState: MMapS ) = copy(mapState = mapState)
  def withText( text: Option[MScSearchText] ) = copy(text = text)
  def withRcvrsGeo( rcvrsGeo: Pot[MGeoNodesResp] ) = copy( rcvrsGeo = rcvrsGeo )
  def withCurrTab( currTab: MSearchTab ) = copy( currTab = currTab )
  def withIsShown( isShown: Boolean ) = copy( isShown = isShown )
  def withMapInitialized(mapReady: Boolean ) = copy( isMapInitialized = mapReady )

}
