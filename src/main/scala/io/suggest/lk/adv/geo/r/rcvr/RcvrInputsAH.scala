package io.suggest.lk.adv.geo.r.rcvr

import diode.data.Pot
import diode.{ActionHandler, ActionResult, ModelR, ModelRW}
import io.suggest.adv.geo.{MRcvrPopupResp, RcvrsMap_t}
import io.suggest.lk.adv.geo.a.SetRcvrStatus

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 16.12.16 15:18
  * Description: Поддержка экшенов (сигналов) и реакции на экшены rcvr-попапа.
  */

class RcvrInputsAH[M](respPot: ModelR[M, Pot[MRcvrPopupResp]],
                      rcvrMapRW: ModelRW[M, RcvrsMap_t]) extends ActionHandler(rcvrMapRW) {

  override protected def handle: PartialFunction[Any, ActionResult[M]] = {

    // Изменилось состояние галочки ресивера в rcvr-попапе.
    case e: SetRcvrStatus =>
      respPot().fold( noChange ) { resp =>
        // Найти узел с текущим id среди всех узлов.
        val checkedOnServerOpt = resp.groups
          .iterator
          .flatMap(_.nodes)
          .find(_.nodeId == e.rcvrKey.to)
          // Содержит ли описание узла с сервера текущее значение чекбокса? Если да, то значит значение галочки вернулось на исходное.
          .map(_.checked)

        val rcvrMap0 = value
        val rcvrMap1 = if ( checkedOnServerOpt.contains(e.checked) ) {
          rcvrMap0 - e.rcvrKey
        } else {
          rcvrMap0 + (e.rcvrKey -> e.checked)
        }

        // TODO Нужно прямо/косвенно запустить effect пересчёта стоимости размещения.
        updated(rcvrMap1)
      }
  }

}
