package io.suggest.sc.sjs.c.scfsm.search

import io.suggest.sc.sjs.c.scfsm.grid.OnGrid
import io.suggest.sc.sjs.c.scfsm.ust.State2UrlT
import io.suggest.sjs.common.fsm.signals.Visible

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 20.05.16 11:13
  * Description: Аддон для состояния раскрытия боковой панели поиска.
  * Изначально код раскрытия жил внутри [[io.suggest.sc.sjs.c.scfsm.grid.Plain]].OnPlayGridStateT._showSearchClick(),
  * что было слишком уж нелогично.
  */
trait Opening extends OnGrid with State2UrlT {

  /** Интерфейс для получения инстанса состояния нахождения на вкладке поиска.
    * Запрашиваемое значение вкладки лежит в _stateData.search.tab.
    */
  trait ISearchPanelOpenedState {
    def _searchPanelOpenedState: FsmState
  }

  /** Состояние раскрытия боковой панели поиска. */
  trait SearchPanelOpeningStateT extends FsmEmptyReceiverState with ISearchPanelOpenedState {

    override def afterBecome(): Unit = {
      super.afterBecome()

      // Необходимо выполнить раскрытие панели, внести необходимые изменения в UI.
      val sd1 = SearchUtil.show( _stateData )

      sd1.search.fsm ! Visible(true)

      // Размыть фоновую плитку, если узкий экран.
      _maybeBlurGrid(sd1)

      // Сразу же сменить состояние на состояние таба, где сейчас открыта панель поиска.
      _stateData = sd1

      // Переключить состояние, если необходимо.
      val nextState = _searchPanelOpenedState
      if (nextState != null) {
        // Перещелкнуть логику работы на новое состояние согласно текущему search-табу.
        become( _searchPanelOpenedState )
        // Сохранить состояние в URL.
        State2Url.pushCurrState()
      }
    }


  }

}
