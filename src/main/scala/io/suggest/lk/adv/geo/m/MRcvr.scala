package io.suggest.lk.adv.geo.m

import diode.FastEq
import diode.data.Pot
import io.suggest.adv.geo.{MRcvrPopupResp, MRcvrPopupState, RcvrsMap_t}
import io.suggest.sjs.leaflet.marker.Marker

import scala.scalajs.js

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 28.12.16 21:31
  * Description: Корневая модель данных и состояний ресиверов.
  */

object MRcvr {

  /** Поддержка быстрого сравнения содержимого сложного контейнера для diode circuit. */
  implicit object MRcvrFastEq extends FastEq[MRcvr] {
    override def eqv(a: MRcvr, b: MRcvr): Boolean = {
      (a.popupResp eq b.popupResp) &&
        (a.markers eq b.markers) &&
        (a.popupState eq b.popupState) &&
        (a.rcvrsMap eq b.rcvrsMap)
    }
  }

}

/** Модель, объединяющая разные модели, относящиеся к ресиверам в форме.
  *
  * @param popupResp Потенциальные данные попапа над маркером (точкой) ресивера.
  *                  Приходят с сервера по запросу, однако сам факт наличия/необходимости
  *                  такого запроса отражается в form.rcvrPopup.
  * @param markers Маркеры карты ресиверов.
  * @param popupState Состояние попапа на ресивере.
  */
case class MRcvr(
                  popupResp   : Pot[MRcvrPopupResp]     = Pot.empty,
                  markers     : Pot[js.Array[Marker]]   = Pot.empty,
                  popupState  : Option[MRcvrPopupState] = None,
                  rcvrsMap    : RcvrsMap_t              = Map.empty
) {

  def withPopupResp(rcvrPopupResp: Pot[MRcvrPopupResp]) = copy(popupResp = rcvrPopupResp)
  def withMarkers(rcvrMarkers2: Pot[js.Array[Marker]]) = copy(markers = rcvrMarkers2)
  def withPopupState(rcvrPopup2: Option[MRcvrPopupState]) = copy(popupState = rcvrPopup2)
  def withRcvrMap(rcvrsMap2: RcvrsMap_t) = copy(rcvrsMap = rcvrsMap2)

}
