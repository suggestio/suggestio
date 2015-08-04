package io.suggest.sc.sjs.vm.util

import io.suggest.sc.sjs.c.ScFsm
import io.suggest.sc.sjs.m.mfsm.IFsmEventMsgCompanion
import io.suggest.sc.sjs.v.vutil.OnClickSelfT
import io.suggest.sjs.common.view.safe.evtg.SafeEventTargetT
import org.scalajs.dom.Event

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 04.08.15 17:49
 * Description: Аддон для поддержки навешивания onClick-листенера при инициализации.
 * Подмешивается к экземплярам кнопок.
 */

trait InitOnClickToFsmT extends IInitLayout with OnClickSelfT with SafeEventTargetT {

  /** Статический компаньон модели для сборки сообщений. */
  protected[this] def _msgModel: IFsmEventMsgCompanion

  /** Инициализация текущей и подчиненных ViewModel'ей. */
  override def initLayout(): Unit = {
    onClick { e: Event =>
      ScFsm ! _msgModel(e)
    }
  }
}
