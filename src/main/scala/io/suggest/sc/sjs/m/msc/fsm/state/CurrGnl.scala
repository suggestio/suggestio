package io.suggest.sc.sjs.m.msc.fsm.state

import io.suggest.sc.sjs.c.NavPanelCtl
import io.suggest.sc.sjs.m.msc.fsm.IScState

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 08.06.15 16:27
 * Description: Аддон для переключения текущего слоя в списке геоузлов.
 */
trait CurrGnl extends IScState {

  override type T <: CurrGnl

  def currGnlIndex: Option[Int]

  /** Накатить возможные изменения в currGnlIndex. */
  def applyCurrGnlChanges(oldState: CurrGnl): Unit = {
    val _currGnlIndex = currGnlIndex
    val _oldGnlIndex = oldState.currGnlIndex
    if (_currGnlIndex != _oldGnlIndex) {
      _oldGnlIndex  foreach NavPanelCtl.hideGnl
      _currGnlIndex foreach NavPanelCtl.showGnl
    }
  }

  override def applyChangesSince(oldState: T): Unit = {
    super.applyChangesSince(oldState)
    applyCurrGnlChanges(oldState)
  }

}
