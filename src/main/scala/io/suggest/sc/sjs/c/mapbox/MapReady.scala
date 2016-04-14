package io.suggest.sc.sjs.c.mapbox

import io.suggest.sc.sjs.m.mgeo.IGeoLocSignal
import io.suggest.sc.sjs.vm.mapbox.GlMapVm

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 14.04.16 13:38
  * Description: Аддон для состояний готовности карты к работе.
  */
trait MapReady extends StoreUserGeoLoc {

  /** Трейт состояния готовности к работе. */
  trait MapReadyStateT extends StoreUserGeoLocStateT {

    /** Реакция на получение данных геолокации текущего юзера. */
    override def _handleUserGeoLoc(userGeoLoc: IGeoLocSignal): Unit = {
      super._handleUserGeoLoc(userGeoLoc)
      val sd0 = _stateData
      for (glmap <- sd0.glmap) {
        GlMapVm(sd0.glmap.get)
          .setUserGeoLoc(userGeoLoc.data)
      }
    }

  }

}
