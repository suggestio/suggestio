package io.suggest.sc.sjs.c.scfsm.search.geo

import io.suggest.sc.sjs.c.scfsm.search.Base
import io.suggest.sc.sjs.m.mmap.MapShowing

/** Аддон для сборки состояния нахождения юзера на раскрытой панели поиска со вкладкой географии. */
trait OnGeo extends Base {

  /** Заготовка состояния нахождения на вкладке панели поиска. */
  protected trait OnGridSearchGeoStateT extends OnSearchStateT {

    override def afterBecome(): Unit = {
      super.afterBecome()
      for (mapFsm <- _stateData.searchFsm.mapFsm)
        mapFsm ! MapShowing
    }

  }

}
