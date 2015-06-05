package io.suggest.sc.sjs.m.msc.fsm.state

import io.suggest.sc.sjs.c.SearchPanelCtl
import io.suggest.sc.sjs.m.msc.fsm.IScState
import io.suggest.sc.sjs.v.search.SearchPanelView

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 05.06.15 9:48
 * Description: Аддон для добавления поддержки поля searchPanelOpened в состояние выдачи.
 */
trait SearchPanelOpened extends IScState {

  override type T <: SearchPanelOpened

  /** Открыта ли панель поиска (правая)? */
  def searchPanelOpened: Boolean


  def applySearchPanelChanges(oldState: SearchPanelOpened): Unit = {
    val spo = searchPanelOpened
    if (spo != oldState.searchPanelOpened) {
      if (spo) {
        // Открыть панель
        SearchPanelCtl.showPanel()
      } else {
        // Скрыть панель.
        SearchPanelCtl.hidePanel()
      }
    }
  }


  override def applyChangesSince(oldState: T): Unit = {
    super.applyChangesSince(oldState)
    applySearchPanelChanges(oldState)
  }

}
