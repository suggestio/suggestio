package io.suggest.sc.m.search

import io.suggest.sc.m.ISc3Action
import io.suggest.sc.sc3.MSc3TagsResp

import scala.util.Try

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


/** Клик по тегу. */
case class TagClick( nodeId: String ) extends ISearchAction

/** Сброс списка тегов. Если теги открыты сейчас, то они должны быть перезагружены. */
case object ResetTags extends ISearchAction

/** Экшен для запуска поиска тегов под текущую выдачу. */
case class GetMoreTags(clear: Boolean, ignorePending: Boolean = false) extends ISearchAction

/** Экшен получения результата запроса поиска тегов. */
case class MoreTagsResp(reason: GetMoreTags, timestamp: Long, reqLimit: Int, resp: Try[MSc3TagsResp]) extends ISearchAction

/** Происходит скроллинг в списке тегов. Возможно, надо подгрузить ещё тегов. */
case class TagsScroll(scrollTop: Double, scrollHeight: Int) extends ISearchAction


/** Изменения фокуса на input'е текстового поиска. */
case class SearchTextFocus(focused: Boolean) extends ISearchAction

/** Происходит ввод текста в поисковый input. */
case class SearchTextChanged(newText: String) extends ISearchAction

/** Таймаут фильтрации быстрых нажатий в поле ввода текста. */
case class SearchTextTimerOut(timestamp: Long) extends ISearchAction

/** Принудительный запуск поиска на текущей поисковой вкладке. */
case object ReDoSearch extends ISearchAction


/** Срабатывание таймера запуска реакции на действия на карте. */
case class MapDelayTimeOut( gen: Long ) extends ISearchAction

/** Команда к запуску index-реакции на текущее состояние гео.карты. */
case class MapReIndex( rcvrId: Option[String] ) extends ISc3Action