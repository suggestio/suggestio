package io.suggest.sc.sjs.c.mapbox

import io.suggest.sc.sjs.vm.mapbox.GlMapVm
import io.suggest.sjs.mapbox.gl.event.MapEventsTypes

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 14.04.16 14:30
  * Description: Аддон для сборки состояний
  */
trait MapInitializing extends StoreUserGeoLoc {

  /** Трейт для сборки состояния ожидания инициализации карты. */
  trait MapInitializingStateT extends StoreUserGeoLocStateT {

    case object MapInitDone

    override def afterBecome(): Unit = {
      super.afterBecome()
      // Повесить событие ожидания инициализации карты.
      val glmap = GlMapVm( _stateData.glmap.get )
      glmap.on( MapEventsTypes.STYLE_LOADED ) { ed =>
        _sendEventSync(MapInitDone)
      }
    }

    override def receiverPart: Receive = super.receiverPart.orElse {
      case MapInitDone =>
        _handleMapInitDone()
    }

    /** Реакция на окончание инициализации на стороне карты. */
    def _handleMapInitDone(): Unit = {
      become(mapReadyState)
    }

    /** Состояние готовности инициализированной карты. */
    def mapReadyState: FsmState

  }

}
