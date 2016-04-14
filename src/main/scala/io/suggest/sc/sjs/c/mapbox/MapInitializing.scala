package io.suggest.sc.sjs.c.mapbox

import io.suggest.sc.sjs.vm.mapbox.GlMapVm
import io.suggest.sjs.mapbox.gl.event._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 14.04.16 14:30
  * Description: Аддон для сборки состояний
  */
trait MapInitializing extends StoreUserGeoLoc {

  /** Трейт для сборки состояния ожидания инициализации карты. */
  trait MapInitializingStateT extends StoreUserGeoLocStateT {

    /** Внутренний сигнал самому себе о завершении инициализации карты. */

    private lazy val vm = GlMapVm( _stateData.glmap.get )

    override def afterBecome(): Unit = {
      super.afterBecome()
      // Повесить событие ожидания инициализации карты.
      vm.on( MapEventsTypes.STYLE_LOADED )(_mapSignalCallbackF(MapInitDone))
    }

    override def receiverPart: Receive = super.receiverPart.orElse {
      case _: MapInitDone =>
        _handleMapInitDone()
    }

    /** Реакция на окончание инициализации на стороне карты. */
    def _handleMapInitDone(): Unit = {
      vm.glMap.off(MapEventsTypes.STYLE_LOADED)

      // Надо повесить listener'ы событий на карту
      vm.on( MapEventsTypes.MOVE_START )(_mapSignalCallbackF(MoveStart))
      vm.on( MapEventsTypes.MOVE )(_mapSignalCallbackF(Moving))
      vm.on( MapEventsTypes.MOVE_END )(_mapSignalCallbackF(MoveEnd))

      // Переключить состояния
      become(mapReadyState)
    }

    /** Состояние готовности инициализированной карты. */
    def mapReadyState: FsmState

  }

}
