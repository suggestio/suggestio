package io.suggest.sc.sjs.c.search.map

import io.suggest.fsm.StateData
import io.suggest.sc.sjs.m.mmap.MMapSd
import io.suggest.sjs.common.fsm.SjsFsm
import io.suggest.sjs.common.fsm.signals.Visible
import io.suggest.sjs.common.util.ISjsLogger

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 06.10.16 14:39
  * Description: stub для написания трейтов-кусков [[MapFsm]].
  */
trait MapFsmStub
  extends SjsFsm
  with StateData
  with ISjsLogger
{

  override type State_t = FsmState
  override type SD = MMapSd

  /** Ресивер для всех состояний. */
  override protected def allStatesReceiver: Receive = {
    val r1: Receive = {
      case vis: Visible =>
        // TODO пока просто игнорим сообщение о видимости/невидимости вкладки.
    }
    r1.orElse {
      super.allStatesReceiver
    }
  }

}
