package io.suggest.sjs.common.fsm

import io.suggest.sjs.common.vm.evtg.{OnMouseClickT, OnClickT, EventTargetVmT}
import io.suggest.sjs.common.vm.util.IInitLayoutDummy
import org.scalajs.dom.Event

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


// TODO Тут исторически OnClickT, но это связывает руки в реализациях. Надо попробовать переехать на OnMouseClickT...
trait InitOnClickToFsmT extends IInitLayoutDummy with OnClickT with EventTargetVmT with SendEventToFsmUtil with ClickMsgModel {

  /** Инициализация текущей и подчиненных ViewModel'ей. */
  override def initLayout(): Unit = {
    super.initLayout()
    val f = _sendEventF[Event](_clickMsgModel)
    onClick(f)
  }

}


/** Когда FSM передаётся в init как аргумент, инициализация идёт похожим образом. */
trait InitLayoutFsmClickT extends IInitLayoutFsmDummy with OnMouseClickT with EventTargetVmT with ClickMsgModel {
  override def initLayout(fsm: SjsFsm): Unit = {
    super.initLayout(fsm)
    val f = SendEventToFsmUtil.f(fsm, _clickMsgModel)
    onClick(f)
  }
}
