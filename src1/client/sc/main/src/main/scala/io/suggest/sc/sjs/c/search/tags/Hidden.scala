package io.suggest.sc.sjs.c.search.tags

import io.suggest.sc.sjs.m.mgeo.NewGeoLoc
import io.suggest.sc.sjs.m.mtags.MTagsSd
import io.suggest.sc.sjs.vm.search.tabs.htag.StList
import io.suggest.sjs.common.fsm.signals.Visible

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 07.10.16 11:14
  * Description: Трейт для поддержки сборки состояний пребывания [[TagsFsm]] в пустом состоянии.
  * Такое состояние имеет место бОльшую часть жизни этого FSM
  */
trait Hidden extends TagsFsmStub {

  /** Трейт состояния, когда fsm в фоне и без данных. */
  trait HiddenStateT extends FsmState {

    override def receiverPart: Receive = {
      // Сигнал получения новых данных геолокации.
      case _: NewGeoLoc =>
        _handleNewScLoc()

      // Сигнал изменения видимости списка тегов.
      case vis: Visible =>
        _handleVisibityChange(vis)
    }


    /** Реакция на сигнал изменения видимости списка тегов. */
    def _handleVisibityChange(vis: Visible): Unit = {
      if (vis.isVisible)
        become( visibleState )
    }

    /** Состояние нахождения в видимом состоянии. */
    def visibleState: FsmState


    /** Реакция на изменение текущих координат выдачи. */
    def _handleNewScLoc(): Unit = {
      // Если теги уже есть, то очистить их список и состояние FSM.
      val sd0 = _stateData
      if (sd0.isLoadedSomething) {
        // Очистить список тегов в DOM, если там что-то уже было отрендерено.
        if (sd0.loadedCount > 0) {
          for (stList <- StList.find()) {
            stList.clear()
          }
        }
        // Сбросить состояние FSM на исходное.
        _stateData = MTagsSd()
      }
    }

  }

}
