package models.event.search

import io.suggest.ym.model.common.DynSearchArgs
import org.elasticsearch.action.search.SearchRequestBuilder
import org.elasticsearch.search.sort.SortOrder
import models.event.MEvent.DATE_CREATED_ESFN

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 12.02.15 16:47
 * Description: MEvent search: поисковый аддон для поддержки сортировки по дате.
 */
trait WithDateSort extends DynSearchArgs {

  /** false = новые сверху, true = новые снизу, None - без сортировки. */
  def withDateSort: Option[Boolean]

  /**
   * Сборка search-реквеста. Можно переопределить чтобы добавить в реквест какие-то дополнительные вещи,
   * кастомную сортировку например.
   * @param srb Поисковый реквест, пришедший из модели.
   * @return SearchRequestBuilder, наполненный данными по поисковому запросу.
   */
  override def prepareSearchRequest(srb: SearchRequestBuilder): SearchRequestBuilder = {
    val srb1 = super.prepareSearchRequest(srb)
    withDateSort.fold(srb1) { wds =>
      val so = if (wds) SortOrder.ASC else SortOrder.DESC
      srb1.addSort(DATE_CREATED_ESFN, so)
    }
  }

  /** Построение выхлопа метода toString(). */
  override def toStringBuilder: StringBuilder = {
    fmtColl2sb("withDateSort", withDateSort, super.toStringBuilder)
  }
}
