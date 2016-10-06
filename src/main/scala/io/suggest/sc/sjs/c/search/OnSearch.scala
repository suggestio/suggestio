package io.suggest.sc.sjs.c.search

import io.suggest.sc.ScConstants.Search.Fts.START_TIMEOUT_MS
import io.suggest.sc.sjs.m.msearch._
import io.suggest.sc.sjs.vm.search.fts.{SInput, SInputContainer}
import io.suggest.sjs.common.controller.DomQuick
import io.suggest.sjs.common.fsm.signals.{Stop, Visible}
import org.scalajs.dom.FocusEvent

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 05.10.16 14:07
  * Description: Трейт поддержки общих операций между всеми табами поисковой панели:
  * - Обработка поля текстового поиска.
  * - Реакция на внетабовые кнопки панели и прочего.
  */
trait OnSearch extends SearchFsmStub {

  /** Сборка состояния на панели поиска. */
  trait OnSearchStateT extends FsmEmptyReceiverState {

    // Ресивер состояний содержит поддержку FTS, переключения на другой таб, прочие события.
    override def receiverPart: Receive = super.receiverPart.orElse {
      // Получение сигналов кликов по кнопкам вкладок.
      case tabBtnClick: ITabClickSignal =>
        _tabBtnClick(tabBtnClick)
      // ScFsm или ещё кто-то командует переключаться принудительно на новый таб.
      case tabBtnSignal: ITabSignal =>
        _tabBtnSwitchSignal(tabBtnSignal)

      // Получение сигналов полнотекстового поиска.
      case FtsFieldFocus(event) =>
        _ftsFieldFocus(event)
      case ffb: FtsFieldBlur =>
        _ftsFieldBlur(ffb)
      case ffku: FtsFieldKeyUp =>
        _ftsKeyUp(ffku)
      case signal: FtsStartRequestTimeout =>
        _ftsStartRequestTimeout(signal)

      // Реакция на сигналы видимости этой поисковой панели из ScFsm
      case vis: Visible =>
        _handleSearchVisible(vis)

      // В финале -- обработка сигнала остановки.
      case stop: Stop =>
        _handleStopSignal(stop)
    }


    /** Обработка кликов по кнопкам поисковых вкладок. */
    protected def _tabBtnClick(signal: ITabClickSignal): Unit = {
      _tabBtnSwitchSignal(signal)
      // TODO Уведомить ScFsm, чтобы выставил новое значение в URL-состояние?
    }
    protected def _tabBtnSwitchSignal(signal: ITabSignal): Unit = {
      _maybeSwitchTab( signal.mtab )
    }


    /** Реакция на сигнал видимости/невидимости панели свыше. */
    protected def _handleSearchVisible(vis: Visible): Unit = {
      // Уведомить текущую панель о наступлении видимости/невидимости
      _notifyTabFsm( vis, _stateData.currTab )
      // Далее, тут можно перейти в состояние ничего-не-деланья если visible=false, но надо ли?
    }


    /** Обновление DOM и состояния FSM при переключении на указанный таб.
      * Если tab не менялся, то ничего не произойдёт. */
    def _maybeSwitchTab(mtab: MTab, sd0: SD = _stateData): Unit = {
      val prevTab = sd0.currTab   // _nowOnTab было ранее.
      // Если текущая вкладка не изменилась, то не делать вообще ничего.
      if (mtab != prevTab) {
        // Сокрыть текущую вкладку, деактивировать её кнопку.
        for (tbody <- prevTab.vmBodyCompanion.find()) {
          tbody.hide()
        }
        for (tbtn <- prevTab.vmBtnCompanion.find()) {
          tbtn.deactivate()
        }
        // Активировать связанную с этим сигналом кнопку вкладки.
        // TODO Opt вместо поиска по id можно извлечь необходимый div из события: target/currentTarget.
        for (tBtn <- mtab.vmBtnCompanion.find()) {
          tBtn.activate()
        }
        for (tBody <- mtab.vmBodyCompanion.find()) {
          tBody.show()
        }
        // Сохранить изменения в состояние.
        _stateData = sd0.copy(
          currTab = mtab
        )

        // Уведомить FSM старого таба о сокрытии оного.
        _notifyTabFsmVisibility(prevTab, isBecomingVisible = false)
        // Уведомить FSM нового таба о появлении таба на экране.
        _notifyTabFsmVisibility(mtab, isBecomingVisible = true)
      }
    }


    /** Сигнал появления фокуса в поле полнотекстового поиска. */
    protected def _ftsFieldFocus(event: FocusEvent): Unit = {
      val sd0 = _stateData
      if (sd0.fts.isEmpty) {
        // первый переход на поле. Нужно активировать его визуально.
        for (inputCont <- SInputContainer.find()) {
          inputCont.activate()
        }
        // Раньше оно зачем-то жило в afterBecome().
        _stateData = sd0.copy(
          fts = Some( MFtsFsmState() )
        )
      }
    }

    /** Сигнал набора символов в поле полнотекстового поиска. */
    protected def _ftsKeyUp(ffku: IFtsFieldKeyUp): Unit = {
      // Надо создать таймер отправки запроса. Но сначала готовим самое начальное состояние.
      val sd0 = _stateData
      for {
        state0 <- sd0.fts
        sinput <- SInput.findUsing(ffku)
      } {
        val q2 = sinput.getNormalized
        // Если изменился текст запроса...
        if (state0.q != q2) {
          // то надо запустить таймера запуска поискового запроса, отменив предыдущий таймер.
          for (timerId <- state0.reqTimerId) {
            DomQuick.clearTimeout(timerId)
          }
          val gen2 = MFtsFsmState.getGeneration
          val newTimerId = DomQuick.setTimeout(START_TIMEOUT_MS) { () =>
            _sendEvent( FtsStartRequestTimeout(gen2))
          }

          // Залить результаты работы в состояние FSM.
          val state2 = state0.copy(
            q           = q2,
            reqTimerId  = Some(newTimerId),
            generation  = gen2
          )

          _stateData = sd0.copy(
            fts = Some(state2)
          )
        }
      }
    }

    /** Реакция на наступеление таймаута ожидания ввода поля текстового поиска. */
    protected def _ftsStartRequestTimeout(signal: FtsStartRequestTimeout): Unit = {
      if ( _stateData.fts.exists(_.generation == signal.generation) ) {
        // Отправить сигнал FSM текущего таба о поисковом запросе.
        for {
          sinput <- SInput.find()
        } {
          _notifyTabFsm(
            Fts(
              query = sinput.getText
            )
          )
        }
      }
    }

    /** Сигнал потери фокуса в поле полнотекстового поиска. */
    protected def _ftsFieldBlur(ffb: IFtsFieldBlur): Unit = {
      // Если текста в поле нет, то деактивировать поле и сбросить поиск.
      for {
        sinput    <- SInput.findUsing(ffb)
        if sinput.getNormalized.isEmpty
        inputCont <- sinput.container
      } {
        inputCont.deactivate()
        val sd0 = _stateData
        _stateData = sd0.copy(
          fts = None
        )
      }
    }

    protected def _handleStopSignal(s: Stop): Unit = {
      val sd0 = _stateData
      // Отфорвардить сигнал остановки в подчинённые FSM.
      for ((_, fsm) <- sd0.tabs) {
        fsm ! s
      }
      // И забыть обо всём...
      _stateData = sd0.copy(
        tabs = Map.empty,
        fts  = None
      )
    }

  }

}
