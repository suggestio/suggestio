package io.suggest.sc.sjs.c.scfsm

import io.suggest.sc.sjs.m.mhdr.{ShowIndexClick, HideSearchClick}
import io.suggest.sc.sjs.m.msearch.{MTabs, MTab, ITabClickSignal}
import io.suggest.sc.sjs.vm.hdr.HRoot
import io.suggest.sc.sjs.vm.search.SRoot
import org.scalajs.dom.Event

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 06.08.15 13:52
 * Description: FSM-Аддон для добавления поддержки состояния выдачи, когда доступна плитка и открыта панель поиска.
 */
trait OnGridSearch extends OnGrid {

  protected trait OnGridSearchStateT extends OnGridStateT with PanelGridRebuilder {

    protected def _nowOnTab: MTab

    /** Метод содержит логику обработки клика по кнопке сокрытия поисковой панели. */
    protected def _hideSearchClick(evt: Event): Unit = {
      val sd0 = _stateData
      for (sroot <- SRoot.find(); screen <- sd0.screen) {
        // Показать панель
        sroot.hide()
        // Сменить набор кнопок в заголовке.
        for (header <- HRoot.find()) {
          header.hideBackToIndexBtns()
        }

        // Отребилдить плитку карточек, создав новое состояние выдачи.
        val grid2 = _rebuildGridOnPanelChange(sd0, screen, sroot)

        val sd1 = sd0.copy(
          grid = grid2,
          search = sd0.search.copy(
            opened = false
          )
        )

        // Сменить состояние на то, где открыта панель поиска.
        become(_nextStateSearchPanelClosed(sd1), sd1)
      }
    }

    /** Состояние FSM, на которое надо переключиться при после сокрытия панели. */
    protected def _nextStateSearchPanelClosed(sd1: SD): FsmState


    /** Обработка кликов по кнопкам поисковых вкладок. */
    protected def _tabBtnClick(signal: ITabClickSignal): Unit = {
      val sd0 = _stateData
      // Если текущая вкладка не изменилась, то не делать вообще ничего.
      val prevTab = _nowOnTab
      if (signal.mtab != prevTab) {
        // Сокрыть текущую вкладку, деактивировать её кнопку.
        for (tbody <- prevTab.vmBodyCompanion.find()) {
          tbody.hide()
        }
        for (tbtn <- prevTab.vmBtnCompanion.find()) {
          tbtn.deactivate()
        }
        // Активировать связанную с этим сигналом кнопку вкладки.
        // TODO Opt вместо поиска по id можно извлечь необходимый div из события: target/currentTarget.
        val nextTab = signal.mtab
        for (tBtn <- nextTab.vmBtnCompanion.find()) {
          tBtn.activate()
        }
        for (tBody <- nextTab.vmBodyCompanion.find()) {
          tBody.show()
        }
        // Сохранить изменения в состояние.
        val sd2 = sd0.copy(
          search = sd0.search.copy(
            currTab = nextTab
          )
        )
        become(_tabSwitchedFsmState(sd2), sd2)
      }
    }

    /** На какое состояние надо переключаться при смене поисковой вкладки? */
    protected def _tabSwitchedFsmState(sd2: SD): FsmState

    override def receiverPart: PartialFunction[Any, Unit] = super.receiverPart orElse {
      // Клик по кнопке сокрытия поисковой панели (справа).
      case HideSearchClick(evt) =>
        _hideSearchClick(evt)

      // Клик по кнопке отображения index (слева).
      case ShowIndexClick(evt) =>
        _hideSearchClick(evt)

      // Получение сигналов кликов по кнопкам вкладок.
      case tabBtnClick: ITabClickSignal =>
        _tabBtnClick(tabBtnClick)
    }

  }

}


/** Аддон для сборки состояния нахождения юзера на раскрытой панели поиска со вкладкой географии. */
trait OnGridSearchGeo extends OnGridSearch {
  /** Заготовка состояния нахождения на вкладке панели поиска. */
  protected trait OnGridSearchGeoStateT extends OnGridSearchStateT {
    override protected def _nowOnTab = MTabs.Geo
  }
}



/** Аддон для сборки состояния нахождения юзера на раскрытой панели поиска со вкладкой хеш-тегов. */
trait OnGridSearchHashTags extends OnGridSearch {
  protected trait OnGridSearchHashTagsStateT extends OnGridSearchStateT {
    override protected def _nowOnTab = MTabs.HashTags
  }
}
