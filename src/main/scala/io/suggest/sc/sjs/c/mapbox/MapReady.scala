package io.suggest.sc.sjs.c.mapbox

import io.suggest.sc.sjs.m.mgeo.IGeoLocSignal
import io.suggest.sc.sjs.vm.mapbox.GlMapVm
import io.suggest.sjs.mapbox.gl.event.IMapMoveSignal

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 14.04.16 13:38
  * Description: Аддон для состояний готовности карты к работе.
  */
trait MapReady extends StoreUserGeoLoc {

  trait StoreUpdateUserGeoLocStateT extends StoreUserGeoLocStateT {

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


  /** Трейт состояния готовности к работе. */
  trait MapReadyStateT extends StoreUpdateUserGeoLocStateT {

    override def receiverPart: Receive = super.receiverPart.orElse {
      case mapDragSignal: IMapMoveSignal =>
        become(mapDraggingState)
    }

    def mapDraggingState: FsmState

  }

}
