package io.suggest.sc.sjs.m.msc.fsm.state

import io.suggest.sc.sjs.c.FtsSearchCtl
import io.suggest.sc.sjs.m.msc.fsm.IScState
import io.suggest.sc.sjs.m.msearch.MFtsSearchCtx

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 09.06.15 17:37
 * Description: Состояние полнотекстового поиска. Оно устроено сложно: None | Some(ctx)
 */
trait FtsSearch extends IScState {

  override type T <: FtsSearch

  /** Поле для полнотекстового поиска. */
  def ftsSearch: Option[MFtsSearchCtx]

  /** Накатить изменения состояния полнотекстового поиска. */
  def applyFtsSearchChanges(oldState: FtsSearch): Unit = {
    FtsSearchCtl.maybeFtsStateChanged(oldState.ftsSearch, newState = ftsSearch)
  }


  override def applyChangesSince(oldState: T): Unit = {
    super.applyChangesSince(oldState)
    applyFtsSearchChanges(oldState)
  }

}
