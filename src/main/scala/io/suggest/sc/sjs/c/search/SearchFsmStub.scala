package io.suggest.sc.sjs.c.search

import io.suggest.fsm.StateData
import io.suggest.sc.sjs.m.msearch.{MSearchFsmSd, MTab}
import io.suggest.sjs.common.fsm.signals.Visible
import io.suggest.sjs.common.fsm.{IFsmMsg, SjsFsm}
import io.suggest.sjs.common.msg.ErrorMsgs

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 12.04.16 19:12
  * Description: Черновик для сборки кусков и состояний MapBox FSM.
  */
trait SearchFsmStub extends SjsFsm with StateData {

  override type State_t = FsmState
  override type SD = MSearchFsmSd

  protected def _subFsm(mtab: MTab) = _stateData.tabs.get(mtab)

  /** Уведомить FSM табов об изменениях, связанных с их видимостью на экране. */
  protected def _notifyTabFsmVisibility(mtab: MTab, isBecomingVisible: Boolean): Unit = {
    _notifyTabFsm( Visible( isBecomingVisible ), mtab )
  }
  protected def _notifyTabFsm(signal: IFsmMsg, mtab: MTab = _stateData.currTab): Unit = {
    _subFsm(mtab).fold[Unit] {
      error( ErrorMsgs.NO_CHILD_FSM_REQUESTED_FOUND + " " + mtab )
    } { tabFsm =>
      tabFsm ! signal
    }
  }

}
