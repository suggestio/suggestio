package models.adv.search.etg

import io.suggest.model.es.EsModelUtil
import io.suggest.model.search.{Limit, Offset, DynSearchArgs}
import models.adv.MExtTarget
import org.elasticsearch.action.search.SearchRequestBuilder
import org.elasticsearch.index.query.{FilterBuilders, QueryBuilder, QueryBuilders}
import org.elasticsearch.search.sort.SortOrder

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 05.03.15 11:50
 * Description: Поиск по модели [[models.adv.MExtTarget]] осуществляется с помощью всего этого.
 */
trait IExtTargetSearchArgs
  extends AdnId
  with SortByDateCreated
  with Limit
  with Offset


/** Дефолтовая реализация аргументов динамического поиска. */
case class ExtTargetSearchArgs(
  override val adnId            : Option[String] = None,
  override val sortByDate       : Option[SortOrder] = None,
  override val limit            : Int = EsModelUtil.MAX_RESULTS_DFLT,
  override val offset           : Int = 0
)
  extends IExtTargetSearchArgs


/** Поиск по id рекламного узла. */
sealed trait AdnId extends DynSearchArgs {
  def adnId: Option[String]

  override def toEsQueryOpt: Option[QueryBuilder] = {
    val acc0 = super.toEsQueryOpt
    val _adnIdOpt = adnId
    if (_adnIdOpt.isEmpty) {
      acc0
    } else {
      val _adnId = _adnIdOpt.get
      val qb1 = acc0.map { qb =>
        // Фильтровать результаты по adnId
        val filter = FilterBuilders.termFilter(MExtTarget.ADN_ID_ESFN, _adnId)
        QueryBuilders.filteredQuery(qb, filter)
      }
      .getOrElse {
        QueryBuilders.termQuery(MExtTarget.ADN_ID_ESFN, _adnId)
      }
      Some(qb1)
    }
  }

  /** Построение выхлопа метода toString(). */
  override def toStringBuilder: StringBuilder = {
    fmtColl2sb("adnId", adnId, super.toStringBuilder)
  }
}


/** Сортировка поисковых результатов по дате создания. */
sealed trait SortByDateCreated extends DynSearchArgs {
  def sortByDate: Option[SortOrder]

  override def prepareSearchRequest(srb: SearchRequestBuilder): SearchRequestBuilder = {
    val srb1 = super.prepareSearchRequest(srb)
    sortByDate.fold(srb1) { sortOrder =>
      srb1.addSort(MExtTarget.DATE_CREATED_ESFN, sortOrder)
    }
  }

  override def toStringBuilder: StringBuilder = {
    fmtColl2sb(classOf[SortByDateCreated].getSimpleName, sortByDate, super.toStringBuilder)
  }
}
