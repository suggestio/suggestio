package io.suggest.sc.sjs.c.scfsm.search

import io.suggest.sc.sjs.c.scfsm.PanelShowHideT
import io.suggest.sc.sjs.c.scfsm.grid.build.{RebuildGridOnPanelClose, RebuildGridOnPanelOpen}
import io.suggest.sc.sjs.m.msc.MScSd
import io.suggest.sc.sjs.vm.hdr.HRoot
import io.suggest.sc.sjs.vm.search.SRoot

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 07.07.16 17:05
  * Description: Статическая утиль для обслуживания панели поиска.
  */
object SearchUtil extends PanelShowHideT {

  /**
    * Код полной логики отображения панели поиска поверх/сбоку от плитки.
    *
    * @param sd0 Исходные данные состояния ScFsm.
    * @return Обновлённые данные состояния ScFsm.
    */
  override def show(sd0: MScSd): MScSd = {
    // Необходимо выполнить раскрытие панели, внести необходимые изменения в UI.
    val sRootOpt = SRoot.find()

    // Показать панель
    for (sRoot <- sRootOpt) {
      sRoot.show()
    }

    // Сменить набор кнопок в заголовке.
    for (header <- HRoot.find()) {
      header.showBackToIndexBtns()
    }

    // Отребилдить плитку карточек, создав новое _stateData выдачи.
    val grid2 = sRootOpt.fold(sd0.grid) { sRoot =>
      RebuildGridOnPanelOpen(sd0, sRoot).execute()
    }

    sd0.copy(
      search = sd0.search.copy(
        opened = true
      ),
      grid = grid2
    )
  }

  /**
    * Сокрытие панели поиска.
    *
    * @param sd0 Начальное состояние, если есть.
    * @return Новое состояние FSM.
    */
  override def hide(sd0: MScSd): MScSd = {
    val sRootOpt = SRoot.find()
    for (sroot <- sRootOpt) {
      // скрыть панель с глаз.
      sroot.hide()
    }
    // Сменить набор кнопок в заголовке.
    for (header <- HRoot.find()) {
      header.hideBackToIndexBtns()
    }

    // Отребилдить плитку карточек, создав новое состояние выдачи.
    val grid2 = sRootOpt.fold(sd0.grid) { sRoot =>
      RebuildGridOnPanelClose(sd0, sRoot).execute()
    }

    sd0.copy(
      grid   = grid2,
      search = sd0.search.copy(
        opened    = false,
        ftsSearch = None
      )
    )
  }

}
