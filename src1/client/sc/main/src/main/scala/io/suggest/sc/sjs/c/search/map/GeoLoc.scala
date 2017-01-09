package io.suggest.sc.sjs.c.search.map

import io.suggest.sc.sjs.m.mgeo.GlLocation
import io.suggest.sjs.common.fsm.signals.Visible

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 14.04.16 10:50
  * Description: Аддон для сборки состояний, сохраняющих в состояние данные геолокации юзера.
  */
trait GeoLoc extends MapFsmStub {

  /** Подмешать в состояние для сохранение полученных геоданных в состояние. */
  trait HandleGeoLocStateT extends FsmEmptyReceiverState {

    override def receiverPart: Receive = super.receiverPart.orElse {
      case userGeoLoc: GlLocation =>
        _handleUserGeoLoc(userGeoLoc)
      case vis: Visible =>
        if (vis.isVisible)
          _handleMapShowing()
    }

    /** Реакция на получение данных геолокации текущего юзера. */
    def _handleUserGeoLoc(userGeoLoc: GlLocation): Unit = {
      val sd0 = _stateData
      _stateData = sd0.copy(
        lastUserLoc = Some(userGeoLoc.data)
      )
    }

    /** Реакция на начало отображения карты на экране. */
    def _handleMapShowing(): Unit = {
      for (mMapInst <- _stateData.mapInst) {
        // Почему-то карта ошибается с размером, но после ресайза определяет корректно.
        mMapInst.glmap.resize()
      }
    }

  }

}
