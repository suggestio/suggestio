package io.suggest.model.search

import org.elasticsearch.action.search.SearchRequestBuilder
import org.elasticsearch.search.sort.{SortOrder, SortBuilders}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 06.10.15 18:49
 * Description: Базовый трейт для сортировки по некому "имени".
 * Имя ES-поля для поиска не задано тут, поэтому нужно дореализовать его на месте.
 */
trait NameSortBase extends DynSearchArgs {

  /** Абсолютное имя not_analyzed-ES-поля, которое будет использоваться для быстрой сортировки. */
  protected def _NAME_FN: String

  /** Сортировать по названиям? */
  def withNameSort: Option[SortOrder]

  override def prepareSearchRequest(srb: SearchRequestBuilder): SearchRequestBuilder = {
    val srb1 = super.prepareSearchRequest(srb)
    val _wns = withNameSort
    if (_wns.isDefined) {
      val sob = SortBuilders.fieldSort(_NAME_FN)
        .order( _wns.get )
        .unmappedType("string")
      srb1 addSort sob
    }
    srb1
  }

  /** Базовый размер StringBuilder'а. */
  override def sbInitSize: Int = {
    val sis = super.sbInitSize
    if (withNameSort.isDefined)  sis + 18  else  sis
  }

  /** Построение выхлопа метода toString(). */
  override def toStringBuilder: StringBuilder = {
    fmtColl2sb("nameSort", withNameSort, super.toStringBuilder)
  }

}


trait NameSortBaseDflt extends NameSortBase {
  override def withNameSort: Option[SortOrder] = None
}


trait NameSortBaseWrap extends NameSortBase with DynSearchArgsWrapper {
  override type WT <: NameSortBase
  override def withNameSort = _dsArgsUnderlying.withNameSort
}
