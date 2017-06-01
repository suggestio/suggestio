package io.suggest.lk.adv.geo.m

import diode.FastEq
import diode.data.Pot
import io.suggest.adv.geo.RcvrsMap_t
import io.suggest.adv.rcvr.{MRcvrPopupResp, MRcvrPopupS}

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
        (a.rcvrsGeo eq b.rcvrsGeo) &&
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
  * @param rcvrsGeo Маркеры и шейпы карты ресиверов.
  * @param popupState Состояние попапа на ресивере.
  */
case class MRcvr(
                  popupResp   : Pot[MRcvrPopupResp]     = Pot.empty,
                  rcvrsGeo    : Pot[MRcvrsGeo]          = Pot.empty,
                  popupState  : Option[MRcvrPopupS]     = None,
                  rcvrsMap    : RcvrsMap_t              = Map.empty
) {

  def withPopupResp(popupResp: Pot[MRcvrPopupResp]) = copy(popupResp = popupResp)
  def withRcvrsGeo(rcvrsGeo: Pot[MRcvrsGeo]) = copy(rcvrsGeo = rcvrsGeo)
  def withPopupState(popupState: Option[MRcvrPopupS]) = copy(popupState = popupState)
  def withRcvrMap(rcvrsMap: RcvrsMap_t) = copy(rcvrsMap = rcvrsMap)

  def withPopup(resp: Pot[MRcvrPopupResp], state: Option[MRcvrPopupS]) = copy(
    popupResp  = resp,
    popupState = state
  )

}
