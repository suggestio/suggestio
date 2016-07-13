package io.suggest.sc.sjs.c.mapbox

import io.suggest.sc.sjs.vm.mapbox.{AllNodesUrl, GlMapVm}
import io.suggest.sc.sjs.vm.search.tabs.geo.SGeoContent
import io.suggest.sjs.common.msg.WarnMsgs
import io.suggest.sjs.mapbox.gl.event._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 14.04.16 14:30
  * Description: Аддон для сборки состояний
  */
trait MapInit extends StoreUserGeoLoc {

  /** Трейт для сборки состояния ожидания инициализации карты. */
  trait MapInitStateT extends StoreUserGeoLocStateT {

    private lazy val vm = GlMapVm( _stateData.glmap.get )

    override def afterBecome(): Unit = {
      super.afterBecome()

      val sd0 = _stateData

      for (glmap <- sd0.glmap) {
        warn( WarnMsgs.MAPBOXLG_ALREADY_INIT )
        glmap.remove()
      }

      for (cont <- SGeoContent.find()) {
        // Пока div контейнера категорий содержит какой-то мусор внутри, надо его очищать перед использованием.
        cont.clear()

        val map0 = GlMapVm.createNew(
          container = cont,
          useLocation = sd0.lastUserLoc
        )

        // Сохранить карту в состояние FSM.
        _stateData = sd0.copy(
          glmap = Some(map0)
        )
      }

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
        .on( MapEventsTypes.MOVE )(_mapSignalCallbackF(Moving))
        .on( MapEventsTypes.MOVE_END )(_mapSignalCallbackF(MoveEnd))

      // Добавить карту узлов.
      for {
        urlVm <- AllNodesUrl.find()
        url   <- urlVm.value
      } {
        vm.initAllNodes(url)
      }

      // Переключить состояния
      become(_mapReadyState)
    }

    /** Состояние готовности инициализированной карты. */
    def _mapReadyState: FsmState

  }

}
