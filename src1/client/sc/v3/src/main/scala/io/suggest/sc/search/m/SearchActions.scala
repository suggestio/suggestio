package io.suggest.sc.search.m

import io.suggest.sc.m.ISc3Action

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 21.07.17 14:55
  * Description: Экшены поиска.
  */

/** Трейт-маркер для экшенов в search. */
sealed trait ISearchAction extends ISc3Action

/** Команда к проведению инициализации гео.карты поиска. */
case object InitSearchMap extends ISc3Action

/** Переключение панели поиска на указанный таб. */
case class SwitchTab( newTab: MSearchTab ) extends ISearchAction
