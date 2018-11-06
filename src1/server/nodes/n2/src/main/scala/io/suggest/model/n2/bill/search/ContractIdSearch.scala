package io.suggest.model.n2.bill.search

import io.suggest.es.search.{DynSearchArgs, DynSearchArgsWrapper}
import io.suggest.mbill2.m.gid.Gid_t
import io.suggest.model.n2.node.MNodeFields
import org.elasticsearch.index.query.{QueryBuilder, QueryBuilders}

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 06.11.18 15:48
  * Description: Dyn-search аддоны для взаимодействия с полем MNode.billing.contractId.
  */
trait ContractIdSearch extends DynSearchArgs {

  /** contractId содержит любой из перечисленных id. */
  def contractIds: Seq[Gid_t]

  /** contractId содержит/не содержит какое-либо значение. */
  def contractIdDefined: Option[Boolean]


  override def toEsQueryOpt: Option[QueryBuilder] = {
    var qbOpt = super.toEsQueryOpt

    // Обработать contractIds
    val _contractIds = contractIds
    if (_contractIds.nonEmpty) {
      val qbC = QueryBuilders.termsQuery( MNodeFields.Billing.BILLING_CONTRACT_ID_FN, _contractIds: _* )
      qbOpt = qbOpt
        .map { qb0 =>
          QueryBuilders.boolQuery()
            .must(qb0)
            .filter(qbC)
        }
        .orElse {
          Some(qbC)
        }
    }

    // Обработать contractIdExist
    for (isExist <- contractIdDefined) {
      val existQb = QueryBuilders.existsQuery( MNodeFields.Billing.BILLING_CONTRACT_ID_FN )
      var qb1 = QueryBuilders.boolQuery()

      qb1 = if (isExist) qb1.filter(existQb)
            else qb1.mustNot(existQb)

      qbOpt = qbOpt
        .map { qb1.must }
        .orElse {
          Some(qb1)
        }
    }

    qbOpt
  }

}


trait ContractIdSearchDflt extends ContractIdSearch {
  override def contractIds: Seq[Gid_t] = Nil
  override def contractIdDefined: Option[Boolean] = None
}


trait ContractIdSearchWrap extends DynSearchArgsWrapper with ContractIdSearch {
  override type WT <: ContractIdSearch
  override def contractIds          = _dsArgsUnderlying.contractIds
  override def contractIdDefined  = _dsArgsUnderlying.contractIdDefined
}
