package io.suggest.sc.sjs.m.msc.fsm.state

import io.suggest.sc.sjs.c.FtsSearchCtl
import io.suggest.sc.sjs.m.msc.fsm.IScState

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 09.06.15 17:37
 * Description: Состояние полнотекстового поиска. Оно устроено сложно: None | Some(ctx)
 */
trait FtsSearch extends IScState {

  override type T <: FtsSearch

  /** Поле для полнотекстового поиска. */
  def ftsSearch: Option[String]

  /** Накатить изменения состояния полнотекстового поиска. */
  def applyFtsSearchChanges(oldState: FtsSearch): Unit = {
    val oldOpt = oldState.ftsSearch
    val newOpt = ftsSearch
    if (oldOpt.isDefined || newOpt.isDefined) {
      FtsSearchCtl.maybeFtsStateChanged(oldOpt, newState = newOpt)
    }
  }


  override def applyChangesSince(oldState: T): Unit = {
    super.applyChangesSince(oldState)
    applyFtsSearchChanges(oldState)
  }

}
