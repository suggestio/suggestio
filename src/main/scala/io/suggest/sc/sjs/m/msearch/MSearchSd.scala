package io.suggest.sc.sjs.m.msearch

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 05.08.15 9:46
 * Description: Контейнер данных состояния (SD, state data) поискова.
 * @param opened Открыта ли поисковая панель?
 */
case class MSearchSd(
  opened: Boolean = false
)
