package io.suggest.sc.m.search

import io.suggest.common.geom.d2.MSize2di
import io.suggest.maps.m.IMapsAction
import io.suggest.sc.m.inx.IIndexAction
import io.suggest.sc.m.{ISc3Action, IScApiRespReason, IScIndexRespReason}
import io.suggest.sjs.leaflet.map.LMap


/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 21.07.17 14:55
  * Description: Экшены поиска.
  */

/** Трейт-маркер для экшенов в search. */
sealed trait ISearchAction extends ISc3Action


sealed trait IGeoTabAction extends ISc3Action


/** Команда к проведению инициализации гео.карты поиска. */
case object InitSearchMap extends IGeoTabAction

/** Реагировать на окончание инициализации карты. */
case class HandleMapReady(map: LMap) extends IGeoTabAction


/** Клик по тегу.
  * @param nodeId id узла.
  */
case class NodeRowClick(nodeId: String) extends IGeoTabAction

/** Происходит скроллинг в списке тегов. Возможно, надо подгрузить ещё тегов. */
case class NodesScroll(scrollTop: Double, scrollHeight: Int) extends IGeoTabAction


sealed trait ISearchTextAction extends ISc3Action

/** Происходит ввод текста в поисковый input. */
case class SearchTextChanged(newText: String, noWait: Boolean = false) extends ISearchTextAction

/** Таймаут фильтрации быстрых нажатий в поле ввода текста.
  *
  * @param timestamp id таймера для проверки.
  *                  None - значит без таймера, без проверок.
  */
case class SearchTextDo(timestamp: Option[Long] = None) extends ISearchTextAction


/** Срабатывание таймера запуска реакции на действия на карте. */
case class MapDelayTimeOut( gen: Long ) extends IMapsAction

/** Выполнить поиск узлов для гео-вкладки. */
case class DoNodesSearch(clear: Boolean, ignorePending: Boolean) extends IGeoTabAction with IScApiRespReason

/** Обработать измеренный размер списка результатов. */
case class NodesFoundListWh( bounds: MSize2di ) extends IGeoTabAction
