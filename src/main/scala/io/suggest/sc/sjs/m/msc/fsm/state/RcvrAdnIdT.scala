package io.suggest.sc.sjs.m.msc.fsm.state

import io.suggest.sc.sjs.c.NodeCtl
import io.suggest.sc.sjs.m.msc.fsm.IScState

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 04.06.15 18:44
 * Description: Аддон для модели MScState для поддержки поля rcvrAdnId и управления им.
 */
trait RcvrAdnIdT extends IScState {

  override type T <: RcvrAdnIdT

  /** id текущего узла, для которого отображается выдача. */
  def rcvrAdnId : Option[String]


  /** Переключить текущий узел, если изменился. */
  def applyRcvrAdnIdChanges(oldState: RcvrAdnIdT): Unit = {
    val _rcvrAdnId = rcvrAdnId
    if (_rcvrAdnId != oldState.rcvrAdnId) {
      NodeCtl.switchToNode(_rcvrAdnId, isFirstRun = false)
    }
  }

  override def applyChangesSince(oldState: T): Unit = {
    super.applyChangesSince(oldState)
    applyRcvrAdnIdChanges(oldState)
  }

}
