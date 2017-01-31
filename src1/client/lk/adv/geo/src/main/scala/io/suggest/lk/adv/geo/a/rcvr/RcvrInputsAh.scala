package io.suggest.lk.adv.geo.a.rcvr

import diode.data.Pot
import diode.{ActionHandler, ActionResult, ModelR, ModelRW}
import io.suggest.adv.geo.RcvrsMap_t
import io.suggest.adv.rcvr.{IRcvrPopupNode, MRcvrPopupResp}
import io.suggest.lk.adv.geo.a.SetRcvrStatus

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 16.12.16 15:18
  * Description: Поддержка экшенов (сигналов) и реакции на экшены rcvr-попапа.
  */

class RcvrInputsAh[M](respPot: ModelR[M, Pot[MRcvrPopupResp]],
                      rcvrMapRW: ModelRW[M, RcvrsMap_t]) extends ActionHandler(rcvrMapRW) {

  override protected def handle: PartialFunction[Any, ActionResult[M]] = {

    // Изменилось состояние галочки ресивера в rcvr-попапе.
    case e: SetRcvrStatus =>
      respPot().fold( noChange ) { resp =>
        // Найти узел с текущим id среди всех узлов.
        val checkedOnServerOpt = resp.node
          // Можно заменить .flatMap на .get: Ведь если это событие обрабатывается, значит хоть одна нода точно должна быть.
          .flatMap( IRcvrPopupNode.findNode(e.rcvrKey, _) )
          .flatMap(_.checkbox)
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
