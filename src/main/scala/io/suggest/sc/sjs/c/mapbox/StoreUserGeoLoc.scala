package io.suggest.sc.sjs.c.mapbox

import io.suggest.sc.sjs.m.mgeo.GlLocation
import io.suggest.sc.sjs.m.mmap.MapShowing

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 14.04.16 10:50
  * Description: Аддон для сборки состояний, сохраняющих в состояние данные геолокации юзера.
  */
trait StoreUserGeoLoc extends MbFsmStub {

  /** Подмешать в состояние для сохранение полученных геоданных в состояние. */
  trait StoreUserGeoLocStateT extends FsmState with FsmEmptyReceiverState {

    override def receiverPart: Receive = super.receiverPart.orElse {
      case userGeoLoc: GlLocation =>
        _handleUserGeoLoc(userGeoLoc)
      case MapShowing =>
        _handleMapShowing()
    }

    /** Реакция на получение данных геолокации текущего юзера. */
    def _handleUserGeoLoc(userGeoLoc: GlLocation): Unit = {
      _stateData = _stateData.copy(
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
