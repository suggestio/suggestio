package io.suggest.sc.sjs.c.scfsm.search.geo

import io.suggest.sc.sjs.c.mapbox.MbFsm
import io.suggest.sc.sjs.c.scfsm.search.Base
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

    override protected def _nowOnTab = MTabs.Geo

    override protected def _ftsLetsStartRequest(): Unit = {
      // TODO Искать "места" по названиям и другим вещам.
      warn( WarnMsgs.NOT_YET_IMPLEMENTED + " " + getClass.getSimpleName )
    }

  }

}
