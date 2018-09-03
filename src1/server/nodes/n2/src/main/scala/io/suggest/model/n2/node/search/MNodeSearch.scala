package io.suggest.model.n2.node.search

import io.suggest.es.search._
import io.suggest.model.n2.bill.tariff.daily.{TfDailyCurrencySearch, TfDailyCurrencySearchDflt, TfDailyCurrencySearchWrap}
import io.suggest.model.n2.edge.search._
import io.suggest.model.n2.extra.domain.{DomainsSearch, DomainsSearchDflt, DomainsSearchWrap}
import io.suggest.model.n2.extra.search._
import io.suggest.model.n2.node.MNodeFields
import io.suggest.model.n2.node.common.search._
import io.suggest.model.n2.node.meta.search._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 14.09.15 15:56
 * Description: Поисковые трейты для dynSearch по [[io.suggest.model.n2.node.MNode]] закидываются сюда.
 */
trait MNodeSearch
  extends SubSearches
  with FtsAll
  with WithIds
  with DomainsSearch
  with OutEdges
  with TfDailyCurrencySearch
  with ShownTypeId
  with AdnRights
  with AdnIsTest
  with NodeTypes
  with WithoutIds
  with ShowInScNl
  with IsEnabled
  with IsDependent
  with NameSort
  with RandomSort
  with ConstScore
  with Limit
  with Offset
  with DateCreatedSort
{
  override final def esTypes = MNodeFields.ES_TYPE_NAMES
}

/** Реализация [[MNodeSearch]] для упрошения жизни компилятору. */
abstract class MNodeSearchImpl
  extends MNodeSearch


/** Объединенные дефолтовые реализация поисковых критериев [[MNodeSearch]]. */
trait MNodeSearchDflt
  extends MNodeSearch
  with SubSearchesDflt
  with FtsAllDflt
  with WithIdsDflt
  with DomainsSearchDflt
  with OutEdgesDflt
  with TfDailyCurrencySearchDflt
  with ShownTypeIdDflt
  with AdnRightsDflt
  with AdnIsTestDflt
  with NodeTypesDflt
  with WithoutIdsDflt
  with ShowInScNlDflt
  with IsEnabledDflt
  with IsDependentDflt
  with NameSortDflt
  with RandomSortDflt
  with ConstScoreDflt
  with LimitDflt
  with OffsetDflt
  with DateCreatedSortDflt


/** Дефолтовая реализация [[MNodeSearchDflt]].
  * Упрощает жизнь компилятору при сборке недефолтовых классов-реализаций. */
class MNodeSearchDfltImpl
  extends MNodeSearchImpl
  with MNodeSearchDflt


/** Wrapper-реализация поисковых критериев [[MNodeSearch]] узла. */
trait MNodeSearchWrap
  extends MNodeSearch
  with SubSearchesWrap
  with FtsAllWrap
  with WithIdsWrap
  with DomainsSearchWrap
  with OutEdgesWrap
  with TfDailyCurrencySearchWrap
  with ShownTypeIdWrap
  with AdnRightsWrap
  with AdnIsTestWrap
  with NodeTypesWrap
  with WithoutIdsWrap
  with ShowInScNlWrap
  with IsEnabledWrap
  with IsDependentWrap
  with NameSortWrap
  with RandomSortWrap
  with ConstScoreWrap
  with LimitWrap
  with OffsetWrap
  with DateCreatedSortWrap
{
  override type WT <: MNodeSearch
}
