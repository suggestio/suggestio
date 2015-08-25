package io.suggest.sc.sjs.c.scfsm

import io.suggest.sc.sjs.m.mhdr.{ShowIndexClick, HideSearchClick}
import io.suggest.sc.sjs.m.msearch._
import io.suggest.sc.sjs.vm.hdr.HRoot
import io.suggest.sc.sjs.vm.search.SRoot
import io.suggest.sc.sjs.vm.search.fts.{SInputContainer, SInput}
import io.suggest.sjs.common.util.ISjsLogger
import org.scalajs.dom
import org.scalajs.dom.ext.KeyCode
import org.scalajs.dom.raw.HTMLInputElement
import org.scalajs.dom.{FocusEvent, KeyboardEvent}
import io.suggest.sc.ScConstants.Search.Fts.START_TIMEOUT_MS

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 06.08.15 13:52
 * Description: FSM-Аддон для добавления поддержки состояния выдачи, когда доступна плитка и открыта панель поиска.
 */
trait OnGridSearch extends OnGrid with ISjsLogger {

  protected trait OnGridSearchStateT extends OnGridStateT with PanelGridRebuilder {

    protected def _nowOnTab: MTab

    // TODO Нужно скрывать панель при нажатии клавиши ESC. Возможно, ещё и отключать текущий поиск.

    /** Метод содержит логику обработки клика по кнопке сокрытия поисковой панели. */
    protected def _hideSearch(): Unit = {
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
        become(_nextStateSearchPanelClosed, sd1)
      }
    }

    /** Состояние FSM, на которое надо переключиться при после сокрытия панели. */
    protected def _nextStateSearchPanelClosed: FsmState


    override def _onKbdKeyUp(event: KeyboardEvent): Unit = {
      super._onKbdKeyUp(event)
      // по ESC надо закрывать вкладку
      if (event.keyCode == KeyCode.escape) {
        _hideSearch()
      }
    }

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
        become(_tabSwitchedFsmState, sd2)
      }
    }

    /** На какое состояние надо переключаться при смене поисковой вкладки? */
    protected def _tabSwitchedFsmState: FsmState

    private def _receiverPart: Receive = {
      // Клик по кнопке сокрытия поисковой панели (справа).
      case HideSearchClick(evt) =>
        _hideSearch()
      // Клик по кнопке отображения index (слева).
      case ShowIndexClick(evt) =>
        _hideSearch()
      // Получение сигналов кликов по кнопкам вкладок.
      case tabBtnClick: ITabClickSignal =>
        _tabBtnClick(tabBtnClick)
      // Получение сигналов от поля полнотекстового поиска.
      case FtsFieldFocus(event) =>
        _ftsFieldFocus(event)
      case ffb: FtsFieldBlur =>
        _ftsFieldBlur(ffb)
      case ffku: FtsFieldKeyUp =>
        _ftsKeyUp(ffku)
      case FtsStartRequestTimeout(generation2) if _stateData.search.ftsSearch.exists(_.generation == generation2) =>
        _ftsLetsStartRequest()
    }

    override def receiverPart: Receive = {
      _receiverPart orElse super.receiverPart
    }

    /** Сигнал появления фокуса в поле полнотекстового поиска. */
    protected def _ftsFieldFocus(event: FocusEvent): Unit = {
      val sd0 = _stateData
      if (sd0.search.ftsSearch.isEmpty) {
        // первый переход на поле. Нужно активировать его визуально.
        for (inputCont <- SInputContainer.find()) {
          inputCont.activate()
        }
        // Обновить состояние FSM.
        _stateData = __initSdFts(sd0)
      }
    }

    /** Сигнал набора символов в поле полнотекстового поиска. */
    protected def _ftsKeyUp(ffku: IFtsFieldKeyUp): Unit = {
      // Надо создать таймер отправки запроса. Но сначала готовим самое начальное состояние.
      val sd0: SD = {
        val sd00 = _stateData
        sd00.search.ftsSearch.fold {
          warn("W8923")
          __initSdFts(sd00)
        } { _ =>
          sd00
        }
      }
      for {
        state0 <- sd0.search.ftsSearch
        sinput <- SInput.findUsing(ffku)
      } {
        val q2 = sinput.getNormalized
        // Если изменился текст запроса...
        if (state0.q != q2) {
          // то надо запустить таймера запуска поискового запроса, отменив предыдущий таймер.
          for (timerId <- state0.reqTimerId) {
            dom.clearTimeout(timerId)
          }
          val gen2 = MFtsFsmState.getGeneration
          val newTimerId = dom.setTimeout(
            { () => _sendEvent( FtsStartRequestTimeout(gen2)) },
            START_TIMEOUT_MS
          )

          // Залить результаты работы в состояние FSM.
          val state2 = state0.copy(
            q           = q2,
            reqTimerId  = Some(newTimerId),
            generation  = gen2
          )

          _stateData = sd0.copy(
            search = sd0.search.copy(
              ftsSearch = Some(state2)
            )
          )
        }
      }
    }

    /** Запуск поискового запроса в рамках текущего состояния. */
    protected def _ftsLetsStartRequest(): Unit

    /** Заливка начального состояния в переданную _stateData. */
    protected def __initSdFts(sd0: SD): SD = {
      sd0.copy(
        search = sd0.search.copy(
          ftsSearch = Some(MFtsFsmState())
        )
      )
    }

    /** Сигнал потери фокуса в поле полнотекстового поиска. */
    protected def _ftsFieldBlur(ffb: IFtsFieldBlur): Unit = {
      // Если текста в поле нет, то деактивировать поле и сбросить поиск.
      for {
        sinput    <- SInput.findUsing(ffb) if sinput.getNormalized.isEmpty
        inputCont <- sinput.container
      } {
        inputCont.deactivate()
        val sd0 = _stateData
        _stateData = sd0.copy(
          search = sd0.search.copy(
            ftsSearch = None
          )
        )
      }
    }
  }

}


/** Аддон для сборки состояния нахождения юзера на раскрытой панели поиска со вкладкой географии. */
trait OnGridSearchGeo extends OnGridSearch {
  /** Заготовка состояния нахождения на вкладке панели поиска. */
  protected trait OnGridSearchGeoStateT extends OnGridSearchStateT {
    override protected def _nowOnTab = MTabs.Geo

    override protected def _ftsLetsStartRequest(): Unit = {
      // TODO Искать "места" по названиям и другим вещам.
      warn("NYI " + getClass.getSimpleName)
    }
  }
}
