package io.suggest.model.n2.node.meta

import io.suggest.model.search.{DynSearchArgsWrapper, DynSearchArgs}
import org.elasticsearch.action.search.SearchRequestBuilder
import org.elasticsearch.search.sort.{SortBuilders, SortOrder}
import EMNodeMeta.META_NAME_SHORT_NOTOK_ESFN

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 23.09.15 19:42
 * Description: Поддержка сортировки по короткому имени узла.
 */

trait NameSort extends DynSearchArgs {

  /** Сортировать по названиям? */
  def withNameSort: Boolean

  override def prepareSearchRequest(srb: SearchRequestBuilder): SearchRequestBuilder = {
    val srb1 = super.prepareSearchRequest(srb)
    if (withNameSort) {
      val sob = SortBuilders.fieldSort(META_NAME_SHORT_NOTOK_ESFN)
        .order(SortOrder.ASC)
        .unmappedType("string")
      srb1 addSort sob
    }
    srb1
  }

  /** Базовый размер StringBuilder'а. */
  override def sbInitSize: Int = {
    val sis = super.sbInitSize
    if (withNameSort)  sis + 18  else  sis
  }

  /** Построение выхлопа метода toString(). */
  override def toStringBuilder: StringBuilder = {
    val sb1 = super.toStringBuilder
    if (withNameSort)
      sb1.append("\n  withNameSort = ").append(withNameSort)
    sb1
  }
}


trait NameSortDflt extends NameSort {
  override def withNameSort: Boolean = false
}


trait NameSortWrap extends NameSort with DynSearchArgsWrapper {
  override type WT <: NameSort
  override def withNameSort = _dsArgsUnderlying.withNameSort
}
