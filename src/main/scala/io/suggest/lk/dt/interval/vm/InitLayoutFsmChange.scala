package io.suggest.lk.dt.interval.vm

import io.suggest.sjs.common.fsm.{IFsmMsgCompanion, SendEventToFsmUtil, SjsFsm, IInitLayoutFsmDummy}
import io.suggest.sjs.common.vm.evtg.EventTargetVmT
import org.scalajs.dom.Event

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 28.12.15 22:35
 * Description: Трейт быстрого для вешанья change-листенера на элементы с пробросом в FSM.
 */
trait InitLayoutFsmChange extends EventTargetVmT with IInitLayoutFsmDummy {

  /** Компаньон модели заворачивания DOM event'а для отсылки в FSM. */
  protected def _changeSignalModel: IFsmMsgCompanion[Event]

  override def initLayout(fsm: SjsFsm): Unit = {
    super.initLayout(fsm)
    // Подписывание на события select'а периода дат.
    val onChangeF = SendEventToFsmUtil.f(fsm, _changeSignalModel)
    addEventListener("change")(onChangeF)
  }

}
