package io.suggest.model.n2.node.meta.search

import io.suggest.es.search.DynSearchArgs
import io.suggest.model.n2.node.MNodeFields
import org.elasticsearch.action.search.SearchRequestBuilder
import org.elasticsearch.search.sort.SortOrder

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 16.11.15 17:16
  * Description: Сортировка по дате создания карточки.
  */
trait DateCreatedSort extends DynSearchArgs {

  def withDateCreatedSort: Option[SortOrder]

  /**
   * Сборка search-реквеста. Можно переопределить чтобы добавить в реквест какие-то дополнительные вещи,
   * кастомную сортировку например.
   * @param srb Поисковый реквест, пришедший из модели.
   * @return SearchRequestBuilder, наполненный данными по поисковому запросу.
   */
  override def prepareSearchRequest(srb: SearchRequestBuilder): SearchRequestBuilder = {
    val srb1 = super.prepareSearchRequest(srb)
    withDateCreatedSort.fold(srb1) { so =>
      srb1.addSort(MNodeFields.Meta.DATE_CREATED_FN, so)
    }
  }

  /** Построение выхлопа метода toString(). */
  override def toStringBuilder: StringBuilder = {
    fmtColl2sb("dateCreatedSort", withDateCreatedSort, super.toStringBuilder)
  }

}


trait DateCreatedSortDflt extends DateCreatedSort {

  override def withDateCreatedSort: Option[SortOrder] = None

}
