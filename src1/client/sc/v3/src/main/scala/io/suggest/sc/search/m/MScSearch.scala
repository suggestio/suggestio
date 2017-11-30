package io.suggest.sc.search.m

import diode.FastEq
import diode.data.Pot
import io.suggest.maps.nodes.MGeoNodesResp
import japgolly.univeq._
import io.suggest.ueq.JsUnivEqUtil._
import io.suggest.ueq.UnivEqUtil._

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
      (a.mapInit    ===* b.mapInit)  &&
        (a.text     ===* b.text)     &&
        (a.rcvrsGeo ===* b.rcvrsGeo) &&
        (a.currTab  ===* b.currTab)  &&
        (a.isShown  ==* b.isShown)
    }
  }

  implicit def univEq: UnivEq[MScSearch] = {
    UnivEq.derive
  }

}


/** Класс состояния панели поиска.
  *
  * @param mapInit Состояние инициализации карты.
  * @param text Состояние текстового поиска.
  * @param rcvrsGeo Гео-данные ресиверов.
  * @param currTab Текущий таб на панели поиска.
  * @param isShown Открыта ли панель поиска на экране?
  */
case class MScSearch(
                      mapInit             : MMapInitState,
                      text                : Option[MScSearchText] = None,
                      rcvrsGeo            : Pot[MGeoNodesResp]    = Pot.empty,
                      currTab             : MSearchTab            = MSearchTabs.default,
                      isShown             : Boolean               = false
                    ) {

  def withMapInit( mapInit: MMapInitState )         = copy( mapInit = mapInit )
  def withText( text: Option[MScSearchText] )       = copy( text = text )
  def withRcvrsGeo( rcvrsGeo: Pot[MGeoNodesResp] )  = copy( rcvrsGeo = rcvrsGeo )
  def withCurrTab( currTab: MSearchTab )            = copy( currTab = currTab )
  def withIsShown( isShown: Boolean )               = copy( isShown = isShown )

}
