package io.suggest.sc.sjs.c.scfsm.search.geo

import io.suggest.sc.sjs.c.mapbox.MbFsm
import io.suggest.sc.sjs.c.scfsm.search.Base
import io.suggest.sc.sjs.m.mgeo.NewGeoLoc
import io.suggest.sc.sjs.m.mgrid.MGridState
import io.suggest.sc.sjs.m.mmap.MapShowing
import io.suggest.sc.sjs.m.msearch.MTabs
import io.suggest.sjs.common.msg.WarnMsgs

/** Аддон для сборки состояния нахождения юзера на раскрытой панели поиска со вкладкой географии. */
trait OnGeo extends Base {

  /** Заготовка состояния нахождения на вкладке панели поиска. */
  protected trait OnGridSearchGeoStateT extends OnSearchStateT {

    override def afterBecome(): Unit = {
      super.afterBecome()
      MbFsm ! MapShowing
    }


    override def receiverPart: Receive = super.receiverPart.orElse {
      // Сигнал от MbFsm о том, что юзер выбрал на карте новую гео-локацию.
      case newGeoLoc: NewGeoLoc =>
        _handleNewGeoLoc(newGeoLoc)
    }

    override protected def _nowOnTab = MTabs.Geo

    override protected def _ftsLetsStartRequest(): Unit = {
      // TODO Искать "места" по названиям и другим вещам.
      warn( WarnMsgs.NOT_YET_IMPLEMENTED + " " + getClass.getSimpleName )
    }


    /** Реакция на сигнал выбора новой геолокации выдачи. */
    def _handleNewGeoLoc(newGeoLoc: NewGeoLoc): Unit = {
      val sd0 = _stateData
      // Меняем состояние FSM выдачи на новую гео-точку вместо прошлой точки или узла.
      val sd1 = sd0.copy(
        common = sd0.common.copy(
          adnIdOpt  = None,
          geoLocOpt = Some(newGeoLoc.gl)
        ),
        grid = sd0.grid.copy(
          state = MGridState(
            adsPerLoad = sd0.grid.state.adsPerLoad
          )
        )
      )
      _stateData = sd1
      // Запустить плитку.
      _startFindGridAds()
    }

  }

}
