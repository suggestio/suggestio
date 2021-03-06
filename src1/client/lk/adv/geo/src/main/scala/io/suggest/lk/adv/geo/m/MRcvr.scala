package io.suggest.lk.adv.geo.m

import diode.FastEq
import diode.data.Pot
import io.suggest.adv.geo.RcvrsMap_t
import io.suggest.adv.rcvr.MRcvrPopupResp
import io.suggest.maps.m.MRcvrPopupS
import io.suggest.maps.nodes.MGeoNodesResp
import japgolly.univeq.UnivEq
import io.suggest.ueq.JsUnivEqUtil._
import io.suggest.ueq.UnivEqUtil._

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
      (a.popupResp ===* b.popupResp) &&
      (a.popupState ===* b.popupState) &&
      (a.rcvrsGeo ===* b.rcvrsGeo) &&
      (a.rcvrsMap ===* b.rcvrsMap)
    }
  }

  @inline implicit def univEq: UnivEq[MRcvr] = UnivEq.derive

}

/** Модель, объединяющая разные модели, относящиеся к ресиверам в форме.
  *
  * @param popupResp Потенциальные данные попапа над маркером (точкой) ресивера.
  *                  Приходят с сервера по запросу, однако сам факт наличия/необходимости
  *                  такого запроса отражается в form.rcvrPopup.
  * @param rcvrsGeo Данные по маркерам и шейпам карты ресиверов.
  * @param popupState Состояние попапа на ресивере.
  */
case class MRcvr(
                  popupResp                 : Pot[MRcvrPopupResp]     = Pot.empty,
                  rcvrsGeo                  : Pot[MGeoNodesResp]      = Pot.empty,
                  popupState                : Option[MRcvrPopupS]     = None,
                  rcvrsMap                  : RcvrsMap_t              = Map.empty
) {

  def withRcvrsGeo(rcvrsGeo: Pot[MGeoNodesResp]) = copy(rcvrsGeo = rcvrsGeo)
  def withRcvrMap(rcvrsMap: RcvrsMap_t) = copy(rcvrsMap = rcvrsMap)
  def withPopupResp(popupResp: Pot[MRcvrPopupResp]) = copy(popupResp = popupResp)

  def withPopup(resp: Pot[MRcvrPopupResp], state: Option[MRcvrPopupS]) = copy(
    popupResp  = resp,
    popupState = state
  )

}
