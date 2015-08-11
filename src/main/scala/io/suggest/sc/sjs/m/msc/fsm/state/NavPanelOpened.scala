package io.suggest.sc.sjs.m.msc.fsm.state

import io.suggest.sc.sjs.c.{HeaderCtl, NavPanelCtl}
import io.suggest.sc.sjs.m.msc.fsm.IScState

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 05.06.15 14:26
 * Description: Поддержка флага открытости панели навигации.
 */
@deprecated("FSM-MVM", "2015.aug.11")
trait NavPanelOpened extends IScState {

  override type T <: NavPanelOpened

  /** Открыта ли панель навигации? */
  def navPanelOpened: Boolean

  /** Накатить изменения флага navPanelOpened на объективную реальность. */
  def appleNavPanelChanges(oldState: NavPanelOpened): Unit = {
    val npo = navPanelOpened
    if (npo != oldState.navPanelOpened) {
      if (npo) {
        // Открываем панель навигации.
        NavPanelCtl.showPanel()
        HeaderCtl.hideRootBtns()
      } else {
        // Скрываем панель навигации.
        NavPanelCtl.hidePanel()
        HeaderCtl.showRootBtns()
      }
    }
  }

  override def applyChangesSince(oldState: T): Unit = {
    super.applyChangesSince(oldState)
    appleNavPanelChanges(oldState)
  }

}
