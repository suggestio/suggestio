package io.suggest.model.n2.edge.search

import io.suggest.model.search.{DynSearchArgsWrapper, DynSearchArgs}
import org.elasticsearch.action.search.SearchRequestBuilder
import org.elasticsearch.search.sort.{SortBuilders, SortOrder}
import io.suggest.model.n2.edge.MEdge.ORDER_FN

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 23.09.15 14:09
 * Description: Поисковый аддон для поддержки сортировки.
 */
trait Ordering extends DynSearchArgs {

  /** Сортировать ли по полю ordering? Если да, то */
  def sortByOutEdgeOrder: Option[SortOrder]

  /**
   * Управление сортировкой незаданных значений поля ordering.
   * @return true они должны быть в хвосте (по умолчанию).
   *         false - в голове.
   */
  def orderNullsLast: Boolean

  override def prepareSearchRequest(srb: SearchRequestBuilder): SearchRequestBuilder = {
    val srb1 = super.prepareSearchRequest(srb)
    val _ord = sortByOutEdgeOrder
    if (_ord.isDefined) {
      val sob = SortBuilders.fieldSort(ORDER_FN)
        .order(_ord.get)
        .unmappedType("integer")
        .missing( if (orderNullsLast) "_last" else "_first" )
      srb1 addSort sob
    }
    srb1
  }

  override def sbInitSize: Int = {
    val sz0 = super.sbInitSize
    sz0 + (if (sortByOutEdgeOrder.isDefined) 20 else 0)
  }

  override def toStringBuilder: StringBuilder = {
    val _ord = sortByOutEdgeOrder.map { ord =>
      val s = ord.toString
      val fix = "NULLs"
      val delim = ","
      if (orderNullsLast)
        s + delim + fix
      else
        fix + delim + s
    }
    fmtColl2sb("sort", _ord, super.toStringBuilder)
  }

}


/** Дефолтовые значения для полей управления сортировкой в [[Ordering]]. */
trait OrderingDflt extends Ordering {
  override def sortByOutEdgeOrder: Option[SortOrder] = None
  override def orderNullsLast: Boolean = true
}


/** Wrap-реализация полей аддона [[Ordering]]. */
trait OrderingWrap extends Ordering with DynSearchArgsWrapper {
  override type WT <: Ordering
  override def sortByOutEdgeOrder = _dsArgsUnderlying.sortByOutEdgeOrder
  override def orderNullsLast = _dsArgsUnderlying.orderNullsLast
}
