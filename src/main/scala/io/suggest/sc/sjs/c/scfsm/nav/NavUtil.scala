package io.suggest.sc.sjs.c.scfsm.nav

import io.suggest.sc.sjs.c.scfsm.PanelShowHideT
import io.suggest.sc.sjs.c.scfsm.grid.build._
import io.suggest.sc.sjs.m.msc.MScSd
import io.suggest.sc.sjs.vm.hdr.btns.HBtns
import io.suggest.sc.sjs.vm.hdr.btns.nav.HShowNavBtn
import io.suggest.sc.sjs.vm.nav.NRoot

/** Статическая утиль для высокоуровневого управления боковой панелью навигации. */
object NavUtil extends PanelShowHideT {

  /**
    * Сокрытие панели навигации.
    *
    * @param sd0 Начальное состояние, если есть.
    * @return Новое состояние FSM.
    */
  override def hide(sd0: MScSd): MScSd = {
    val nRootOpt = NRoot.find()
    for (nRoot <- nRootOpt) {
      // Визуально скрыть панель на экране.
      nRoot.hide()
    }

    // Скрыть кнопку показа панели.
    for (showBtn <- HShowNavBtn.find()) {
      showBtn.show()
    }

    val grid2 = nRootOpt.fold(sd0.grid) { nRoot =>
      RebuildGridOnPanelClose(sd0, nRoot).execute()
    }

    for (hbtns <- HBtns.find()) {
      hbtns.show()
    }

    sd0.copy(
      grid = grid2,
      nav = sd0.nav.copy(
        panelOpened = false
      )
    )
  }


  /** Полный код сокрытия боковой панели навигации. */
  override def show(sd0: MScSd): MScSd = {
    val nRootOpt = NRoot.find()
    for (nroot <- nRootOpt) {
      // Визуально отобразить панель
      nroot.show()
    }
    // Скрыть кнопку показа панели.
    for (showBtn <- HShowNavBtn.find()) {
      showBtn.hide()
    }

    val grid2 = nRootOpt.fold(sd0.grid) { nRoot =>
      RebuildGridOnPanelOpen(sd0, nRoot).execute()
    }

    for (hbtns <- HBtns.find()) {
      hbtns.hide()
    }
    sd0.copy(
      grid = grid2,
      nav = sd0.nav.copy(
        panelOpened = true
      )
    )
  }

}
