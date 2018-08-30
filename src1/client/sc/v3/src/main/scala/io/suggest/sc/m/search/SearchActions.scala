package io.suggest.sc.m.search

import io.suggest.sc.m.{ISc3Action, IScApiRespReason, IScIndexRespReason}


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


/** Клик по тегу. */
case class NodeRowClick(nodeId: String) extends ISearchAction

/** Происходит скроллинг в списке тегов. Возможно, надо подгрузить ещё тегов. */
case class NodesScroll(scrollTop: Double, scrollHeight: Int) extends ISearchAction


/** Изменения фокуса на input'е текстового поиска. */
case class SearchTextFocus(focused: Boolean) extends ISearchAction

/** Происходит ввод текста в поисковый input. */
case class SearchTextChanged(newText: String) extends ISearchAction

/** Таймаут фильтрации быстрых нажатий в поле ввода текста. */
case class SearchTextTimerOut(timestamp: Long) extends ISearchAction


/** Срабатывание таймера запуска реакции на действия на карте. */
case class MapDelayTimeOut( gen: Long ) extends ISearchAction

/** Команда к запуску index-реакции на текущее состояние гео.карты. */
case class MapReIndex( rcvrId: Option[String] ) extends ISc3Action with IScIndexRespReason

/** Выполнить поиск узлов для гео-вкладки. */
case class DoNodesSearch(clear: Boolean, ignorePending: Boolean) extends ISearchAction with IScApiRespReason
