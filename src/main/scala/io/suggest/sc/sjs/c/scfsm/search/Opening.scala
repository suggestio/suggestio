package io.suggest.sc.sjs.c.scfsm.search

import io.suggest.sc.sjs.c.scfsm.grid.{OnGrid, PanelGridRebuilder}
import io.suggest.sc.sjs.c.scfsm.ust.StateToUrlT
import io.suggest.sc.sjs.vm.hdr.HRoot
import io.suggest.sc.sjs.vm.search.SRoot

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 20.05.16 11:13
  * Description: Аддон для состояния раскрытия боковой панели поиска.
  * Изначально код раскрытия жил внутри [[io.suggest.sc.sjs.c.scfsm.grid.Plain]].OnPlayGridStateT._showSearchClick(),
  * что было слишком уж нелогично.
  */
trait Opening extends OnGrid with StateToUrlT {

  /** Интерфейс для получения инстанса состояния нахождения на вкладке поиска.
    * Запрашиваемое значение вкладки лежит в _stateData.search.tab.
    */
  trait ISearchTabOpenedState {
    def _searchTabOpenedState: FsmState
  }

  /** Состояние раскрытия боковой панели поиска. */
  trait SearchPanelOpeningStateT extends FsmEmptyReceiverState with ISearchTabOpenedState with PanelGridRebuilder {

    override def afterBecome(): Unit = {
      super.afterBecome()

      // Необходимо выполнить раскрытие панели, внести необходимые изменения в UI.
      val sd0 = _stateData
      val sRootOpt = SRoot.find()

      // Показать панель
      for (sroot <- sRootOpt) {
        sroot.show()
      }

      // Сменить набор кнопок в заголовке.
      for (header <- HRoot.find()) {
        header.showBackToIndexBtns()
      }

      // Размыть фоновую плитку, если узкий экран.
      _maybeBlurGrid(sd0)

      // Отребилдить плитку карточек, создав новое _stateData выдачи.
      for (sroot <- sRootOpt) {
        val grid2 = RebuildGridOnPanelOpen(sd0, sroot).execute()

        val sd1 = sd0.copy(
          search = sd0.search.copy(
            opened = true
          ),
          grid = grid2
        )

        // Сразу же сменить состояние на состояние таба, где сейчас открыта панель поиска.
        _stateData = sd1
      }

      // Переключить состояние, если необходимо.
      val nextState = _searchTabOpenedState
      if (nextState != null) {
        // Перещелкнуть логику работы на новое состояние согласно текущему search-табу.
        become(_searchTabOpenedState)
        // Сохранить состояние в URL.
        State2Url.pushCurrState()
      }
    }

  }

}
