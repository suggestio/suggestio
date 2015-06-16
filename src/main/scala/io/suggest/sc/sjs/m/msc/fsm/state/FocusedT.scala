package io.suggest.sc.sjs.m.msc.fsm.state

import io.suggest.sc.sjs.c.FocusedCtl
import io.suggest.sc.sjs.m.msc.fsm.IScState

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 16.06.15 11:44
 * Description: Поддержка состояния для focused-выдачи.
 */
trait FocusedT extends IScState {

  override type T <: FocusedT

  /**
   * Offset просматриваемой (текущей) карточки в рамках focused-выдачи.
   * @return None если focused-выдача отключена, и идёт просмотр плитки.
   *         Some(n = 0,1,2...) если активна focused выдача, и идёт просмотр карточки n от начала списка.
   */
  def focOffset: Option[Int]

  def applyFocOffChanges(oldState: FocusedT): Unit = {
    val oldOff = oldState.focOffset
    val newOff = focOffset
    if (oldOff != newOff) {
      FocusedCtl.handleFocOffChanged(oldOff, newOff = newOff)
    }
  }

  override def applyChangesSince(oldState: T): Unit = {
    super.applyChangesSince(oldState)
    applyFocOffChanges(oldState)
  }

}
