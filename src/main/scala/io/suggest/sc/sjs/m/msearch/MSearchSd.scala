package io.suggest.sc.sjs.m.msearch

import io.suggest.sc.sjs.c.search.SearchFsm

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 05.08.15 9:46
 * Description: Контейнер данных состояния (SD, state data) поисковой вкладки.
 *
 * @param opened Открыта ли поисковая панель?
 */
case class MSearchSd(
  opened        : Boolean               = false
) {

  /** Для будущего class-инстанса SearchFsm здесь поле. */
  def fsm = SearchFsm

}
