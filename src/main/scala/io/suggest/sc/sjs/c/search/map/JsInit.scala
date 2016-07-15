package io.suggest.sc.sjs.c.search.map

import io.suggest.sc.sjs.m.mmap.EnsureMap

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 12.04.16 22:12
  * Description: FSM-аддон для состояния оценки готовности mapbox-gl.js к работе.
  */

trait JsInit extends GeoLoc {

  /** Трейт для сборки состояния готовности mapbox-gl.js к работе на странице. */
  trait MapJsInitStateT extends HandleGeoLocStateT {

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

  }


  /** Трейт поддержки абстрактной реакции на сигнал EnsureMap. */
  trait IMapWaitEnsureHandler extends FsmEmptyReceiverState {

    override def receiverPart: Receive = super.receiverPart.orElse {
      // ScFsm намекает о необходимости убедиться, что карта готова к работе.
      case em: EnsureMap =>
        _handleEnsureMap(em)
    }

    /** Реакция на сигнал EnsureMap. */
    def _handleEnsureMap(em: EnsureMap): Unit

  }


  /** Ожидать сигнал EnsureMap и реагировать на него. */
  trait MapWaitEnsureT extends IMapWaitEnsureHandler {

    override def _handleEnsureMap(em: EnsureMap): Unit = {
      become(_mapInitState)
    }

    /** Состояние полной готовности карты. */
    def _mapInitState: FsmState

  }

}
