package io.suggest.n2.node.search

import io.suggest.es.search._
import io.suggest.n2.bill.search.ContractIdSearch
import io.suggest.n2.bill.tariff.daily.TfDailyCurrencySearch
import io.suggest.n2.edge.search._
import io.suggest.n2.extra.domain.DomainsSearch
import io.suggest.n2.extra.search._
import io.suggest.n2.node.MNodeFields
import io.suggest.n2.node.common.search._
import io.suggest.n2.node.meta.search._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 14.09.15 15:56
 * Description: Поисковые трейты для dynSearch по [[io.suggest.n2.node.MNode]] закидываются сюда.
 */
class MNodeSearch
  extends SubSearches
  with WithIds
  with DomainsSearch
  with OutEdges
  with TfDailyCurrencySearch
  with ShownTypeId
  with AdnRights
  with AdnIsTest
  with NodeTypes
  with WithoutIds
  with IsEnabled
  with IsDependent
  with NameSort
  with RandomSort
  with ConstScore
  with Limit
  with Offset
  with DateCreatedSort
  with ContractIdSearch
{
  override final def esTypes = MNodeFields.ES_TYPE_NAME :: Nil
}

