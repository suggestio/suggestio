package io.suggest.sc.sjs.c.mapbox

import io.suggest.sc.sjs.m.mgeo.{GlLocation, MGeoLoc}
import io.suggest.sc.sjs.m.mmap.EnsureMap
import io.suggest.sc.sjs.vm.mapbox.GlMapVm
import io.suggest.sjs.mapbox.gl.event.IMapMoveSignal
import io.suggest.sjs.mapbox.gl.map.GlMap

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 14.04.16 13:38
  * Description: Аддон для состояний готовности карты к работе.
  */
trait MapReady extends StoreUserGeoLoc {

  /** Сохранение геолокации юзера на какру. */
  trait StoreUpdateUserGeoLocStateT extends StoreUserGeoLocStateT {

    /** Реакция на получение данных геолокации текущего юзера. */
    override def _handleUserGeoLoc(userGeoLoc: GlLocation): Unit = {
      super._handleUserGeoLoc(userGeoLoc)
      val sd0 = _stateData
      for (glmap <- sd0.mapInst) {
        GlMapVm(glmap.glmap)
          .setUserGeoLoc(userGeoLoc.data)
      }
    }

    /** Выставление координат юзера на карту. */
    def _setUserGeoLoc(geoLoc: MGeoLoc, glmap: GlMap): GlMapVm = {
      GlMapVm(glmap)
        .setUserGeoLoc(geoLoc)
    }

  }


  /** Трейт состояния готовности к работе. */
  trait MapReadyStateT extends StoreUpdateUserGeoLocStateT {

    override def receiverPart: Receive = {
      val r: Receive = {
        case mapMoveSignal: IMapMoveSignal =>
          _handleMapMove(mapMoveSignal)
        case _: EnsureMap =>
          _handleEnsureMap()
      }
      r.orElse( super.receiverPart )
    }

    /** Реагирование на начавшееся движение карты. */
    def _handleMapMove(mapMoveSignal: IMapMoveSignal): Unit = {
      // Раз уж началась движуха карты, то надо сбросить флаг map-follow-location.
      val sd0 = _stateData
      if (sd0.followCurrLoc) {
        _stateData = sd0.copy(
          followCurrLoc = false
        )
      }

      // Теперь карта откреплена от локации юзера. Перейти на состояние двигательства карты.
      become(_mapMovingState)
    }


    /** Состояние таскания карты. */
    def _mapMovingState: FsmState

    // При выставлении геолокации следует обновлять центровку карты, если карта следует за локацией юзера.
    override def _setUserGeoLoc(geoLoc: MGeoLoc, glmap: GlMap): GlMapVm = {
      val vm = super._setUserGeoLoc(geoLoc, glmap)

      // Если карта должна следовать за локацией юзера, то так и сделать.
      if (_stateData.followCurrLoc) {
        // TODO Задействовать анимацию бы (panTo, easeTo, flyTo)
        vm.center = geoLoc.point
      }

      vm
    }

    /** Реакция на повторный запрос ensuring'а карты. */
    def _handleEnsureMap(): Unit = {
      become( _mapInitState )
    }

    def _mapInitState: FsmState

  }

}
