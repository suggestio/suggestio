package io.suggest.sc.search.m

import io.suggest.sc.m.ISc3Action
import io.suggest.sc.sc3.MSc3Resp

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
case class GetMoreTags(clear: Boolean) extends ISearchAction

/** Экшен получения результата запроса поиска тегов. */
case class MoreTagsResp(reason: GetMoreTags, timestamp: Long, resp: Try[MSc3Resp]) extends ISearchAction
