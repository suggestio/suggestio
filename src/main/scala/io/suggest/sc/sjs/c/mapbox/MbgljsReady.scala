package io.suggest.sc.sjs.c.mapbox

import io.suggest.sc.sjs.m.mmap.EnsureMap
import io.suggest.sc.sjs.vm.mapbox.GlMapVm
import io.suggest.sc.sjs.vm.search.tabs.geo.SGeoRoot
import io.suggest.sjs.common.msg.WarnMsgs

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 12.04.16 22:12
  * Description: FSM-аддон для состояния готовности js к работе.
  */

// TODO Распилить состояние на несколько: js ужеготов, карта инициализируется, карта инициализирована.

trait MbgljsReady extends StoreUserGeoLoc {

  /** Трейт для сборки состояния готовности mapbox-gl.js к работе на странице. */
  trait MbgljsReadyStateT extends StoreUserGeoLocStateT {

    override def afterBecome(): Unit = {
      super.afterBecome()
      // если в состоянии есть несвоевременные сообщения, то отработать их.
      val sd0 = _stateData
      val earlyMsgs = sd0.early
      if (earlyMsgs.nonEmpty) {
        _stateData = sd0.copy(
          early = Nil
        )
        for (em <- earlyMsgs.reverseIterator) {
          _sendEvent(em)
        }
      }
    }

    override def receiverPart: Receive = super.receiverPart.orElse {
      // ScFsm намекает о необходимости убедиться, что карта готова к работе.
      case em: EnsureMap =>
        _ensureMap(em)
    }


    /** Инициализация карты в текущей выдаче, если необходимо. */
    def _ensureMap(em: EnsureMap): Unit = {
      val sd0 = _stateData

      for (glmap <- sd0.glmap) {
        warn( WarnMsgs.MAPBOXLG_ALREADY_INIT )
        glmap.remove()
      }

      for (cont <- SGeoRoot.find()) {
        // Пока div контейнера категорий содержит какой-то мусор внутри, надо его очищать перед использованием.
        cont.clear()

        val map0 = GlMapVm.createNew(
          container = cont,
          useLocation = sd0.lastUserLoc
        )

        // Сохранить карту в состояние FSM.
        val sd1 = sd0.copy(
          glmap   = Some(map0)
        )

        become(_mapInitializingState, sd1)
      }
    }

    /** Состояние полной готовности карты. */
    def _mapInitializingState: FsmState

  }

}
