package io.suggest.sjs.common.fsm

import io.suggest.sjs.common.vm.evtg.{OnClickT, EventTargetVmT}
import io.suggest.sjs.common.vm.util.IInitLayoutDummy
import org.scalajs.dom.Event

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 07.09.15 17:04
 * Description:
 */
trait InitOnClickToFsmT extends IInitLayoutDummy with OnClickT with EventTargetVmT with SendEventToFsmUtil {

  /** Статический компаньон модели для сборки сообщений. */
  protected[this] def _clickMsgModel: IFsmEventMsgCompanion

  /** Инициализация текущей и подчиненных ViewModel'ей. */
  override def initLayout(): Unit = {
    super.initLayout()
    val f = _sendEventF[Event](_clickMsgModel)
    onClick(f)
  }

}
