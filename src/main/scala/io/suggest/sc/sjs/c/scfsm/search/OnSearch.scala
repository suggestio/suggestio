package io.suggest.sc.sjs.c.scfsm.search

import io.suggest.sc.sjs.c.scfsm.grid.OnGrid
import io.suggest.sc.sjs.c.scfsm.ust.State2UrlT
import io.suggest.sc.sjs.c.search.SearchFsm
import io.suggest.sc.sjs.m.mgeo.NewGeoLoc
import io.suggest.sc.sjs.m.mgrid.MGridState
import io.suggest.sc.sjs.m.mhdr.{HideSearchClick, LogoClick, ShowIndexClick}
import io.suggest.sc.sjs.m.msc.MScSd
import io.suggest.sc.sjs.m.msearch._
import io.suggest.sc.sjs.m.mtags.TagSelected
import io.suggest.sjs.common.fsm.signals.Visible
import org.scalajs.dom.ext.KeyCode
import org.scalajs.dom.KeyboardEvent

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 06.08.15 13:52
 * Description: FSM-Аддон для добавления поддержки состояния выдачи, когда доступна плитка и открыта панель поиска.
 */
trait OnSearch extends OnGrid with State2UrlT {

  protected trait OnSearchStateT extends OnGridStateT {

    /** Метод содержит логику обработки клика по кнопке сокрытия поисковой панели. */
    protected def _hideSearchPanel(): Unit = {
      val sd0 = _stateData
      _unBlurGrid()
      val sd1 = SearchUtil.hide(sd0)
      sd0.search.fsm ! Visible(false)
      // Сменить состояние на то, где открыта панель поиска.
      become(_nextStateSearchPanelClosed, sd1)
      State2Url.pushCurrState()
    }

    /** Состояние FSM, на которое надо переключиться при после сокрытия панели. */
    protected def _nextStateSearchPanelClosed: FsmState


    override def _onKbdKeyUp(event: KeyboardEvent): Unit = {
      super._onKbdKeyUp(event)
      // по ESC надо закрывать вкладку
      if (event.keyCode == KeyCode.Escape) {
        _hideSearchPanel()
      }
    }

    override def receiverPart: Receive = {
      val _receiverPart: Receive = {
        // Клик по кнопке сокрытия поисковой панели (справа).
        case _: HideSearchClick | ShowIndexClick | LogoClick =>
          _hideSearchPanel()
        case newGeoLoc: NewGeoLoc =>
          _handleNewGeoLoc(newGeoLoc)
        // Клик по кнопке отображения index (слева).
        case _: ShowIndexClick =>
          _hideSearchPanel()
        // Клик по логотипу наверху экрана.
        case _: LogoClick =>
          _hideSearchPanel()
        // Сигнал об изменении конфигурации тегов
        case tagSel: TagSelected =>
          _handleTagSelected(tagSel)
      }
      _receiverPart.orElse( super.receiverPart )
    }


    /** Реакция на сигнал выбора новой геолокации выдачи. */
    def _handleNewGeoLoc(newGeoLoc: NewGeoLoc): Unit = {
      val sd0 = _stateData
      // Меняем состояние FSM выдачи на новую гео-точку вместо прошлой точки или узла.
      val sd1 = sd0.copy(
        common = sd0.common.copy(
          adnIdOpt  = None,
          geoLocOpt = Some(newGeoLoc)
        ),
        grid = sd0.grid.copy(
          state = MGridState(
            adsPerLoad = sd0.grid.state.adsPerLoad
          )
        )
      )
      _stateData = sd1
      // Запустить плитку.
      _startFindGridAds()

      // Уведомить список геотегов о потере актуальности этого самого списка.
      for (tagsFsm <- sd1.search.fsm.tagsFsm) {
        tagsFsm ! newGeoLoc
      }
    }


    /**
      * Юзер гуляет по истории браузера внутри сеанса выдачи.
      * Необходимо сократить путь этого гуляния без перезагрузки выдачи.
      *
      * @param sdNext Распарсенные данные состояния из URL.
      */
    override def _handleStateSwitch(sdNext: MScSd): Unit = {
      // Изменился текущий tab, выполнить переключение таба на экране и в sd0
      val mtabNext = sdNext.search.currTab
      SearchFsm ! MTabSwitchSignal( mtabNext )
      val noFoc = sdNext.focused.isEmpty

      if (sdNext.search.opened && noFoc) {
        // Уведомить SearchFSM о переключении на указанный таб...
        _stateData.search.fsm ! MTabSwitchSignal(mtabNext)

      } else  if (noFoc && !sdNext.nav.panelOpened) {
        // Возврат на голую плитку.
        _hideSearchPanel()
      } else {
        // Какое-то сложное переключение состояния...
        super._handleStateSwitch(sdNext)
      }
    }


    /** Реакция на сигнал о выборе тега. */
    def _handleTagSelected(tagSel: TagSelected): Unit = {
      // Обновить состояние
      val sd0 = _stateData
      val sd1 = sd0.copy(
        common = sd0.common.copy(
          tagOpt = tagSel.info
        ),
        grid = sd0.grid.copy(
          state = MGridState(
            adsPerLoad = sd0.grid.state.adsPerLoad
          )
        )
      )
      _stateData = sd1

      // Запустить ре-рендер плитки...
      _startFindGridAds()
    }

  }

}
