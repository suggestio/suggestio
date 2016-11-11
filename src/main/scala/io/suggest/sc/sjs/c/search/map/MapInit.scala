package io.suggest.sc.sjs.c.search.map

import io.suggest.sc.sjs.m.mmap.MMapInst
import io.suggest.sc.sjs.vm.mapbox.{AllNodesUrl, GlMapVm}
import io.suggest.sc.sjs.vm.search.tabs.geo.SGeoContent
import io.suggest.sjs.common.msg.ErrorMsgs
import io.suggest.sjs.mapbox.gl.event._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 14.04.16 14:30
  * Description: Аддон для сборки состояний
  */
trait MapInit extends GeoLoc with Early {

  protected def _mapSignalCallbackF(model: IMapSignalCompanion[_]) = {
    {arg: EventData =>
      _sendEventSyncSafe( model(arg) )
    }
  }

  /** Трейт для сборки состояния ожидания инициализации карты. */
  trait MapInitStateT extends HandleGeoLocStateT with HandleAll2Early {

    override def afterBecome(): Unit = {
      super.afterBecome()
      val sd0 = _stateData

      for (currCont <- SGeoContent.find()) {
        sd0.mapInst.fold[Unit] {
          // Мапы нет вообще, надо инициализировать.
          val glmap = GlMapVm.createNew(
            container   = currCont,
            useLocation = sd0.lastUserLoc
          )
          val mmi = MMapInst(glmap, currCont)
          // Сохранить новый инстанс карты в состояние FSM.
          _stateData = sd0.copy(
            mapInst = Some(mmi)
          )
          // Подписать FSM на события готовности карты к работе.
          GlMapVm(glmap)
            .on( MapEventsTypes.STYLE_LOADED )(_mapSignalCallbackF(MapInitDone))

        } { inst =>
          // Мапа уже есть. Убедится, что она есть в контейнере.
          if (currCont.isEmpty) {
            // Пустой контейнер карты надобно заменить на имеющийся в состоянии.
            currCont.replaceWith(inst.container)
          }
          _done()
        }
      }
    }


    override def receiverPart: Receive = {
      val r: Receive = {
        case _: IMapInitDone =>
          _handleMapInitDone()
      }
      r.orElse( super.receiverPart )
    }

    /** Реакция на окончание инициализации на стороне карты. */
    def _handleMapInitDone(): Unit = {
      for (mMapInst <- _stateData.mapInst) {
        val vm = GlMapVm( mMapInst.glmap )
        vm.glMap.off(MapEventsTypes.STYLE_LOADED)

        // Надо повесить listener'ы событий на карту
        vm.on(MapEventsTypes.MOVE_START)(_mapSignalCallbackF(MoveStart))
          .on(MapEventsTypes.MOVE_END)(_mapSignalCallbackF(MoveEnd))

        // Добавить карту узлов.
        for {
          urlVm <- AllNodesUrl.find()
          url   <- urlVm.value
        } {
          vm.initAllNodes(url)
        }
      }

      _done()
    }

    /** Переключить состояние в готовность карты к работе. */
    private def _done(): Unit = {
      become(_mapReadyState)
    }

    /** Состояние готовности инициализированной карты. */
    def _mapReadyState: FsmState

    override def processFailure(ex: Throwable): Unit = {
      error( ErrorMsgs.MAP_INIT_FAILED, ex )
      _done()
    }
  }

}
