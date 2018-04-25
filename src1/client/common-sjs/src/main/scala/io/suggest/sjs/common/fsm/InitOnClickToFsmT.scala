package io.suggest.sjs.common.fsm

import io.suggest.sjs.common.vm.evtg.{EventTargetVmT, OnMouseClickT}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 07.09.15 17:04
 * Description:
 */

trait ClickMsgModel {

  /** Статический компаньон модели для сборки сообщений. */
  protected[this] def _clickMsgModel: IFsmEventMsgCompanion
}


/** Когда FSM передаётся в init как аргумент, инициализация идёт похожим образом. */
trait InitLayoutFsmClickT extends IInitLayoutFsmDummy with OnMouseClickT with EventTargetVmT with ClickMsgModel {
  override def initLayout(fsm: SjsFsm): Unit = {
    super.initLayout(fsm)
    val f = SendEventToFsmUtil.f(fsm, _clickMsgModel)
    onClick(f)
  }
}
