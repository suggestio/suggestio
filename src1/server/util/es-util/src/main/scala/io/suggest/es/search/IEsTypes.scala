package io.suggest.es.search

import io.suggest.es.util.SioEsUtil
import org.elasticsearch.index.query.{QueryBuilder, QueryBuilders}

/** Interface Elasticsearch _type values. */
trait IEsTypes {

  /** List of _types. */
  def esTypes: Seq[String]

}


/** Filtering by deprecated (since ES-6.0) _type field values. */
trait EsTypesFilter extends DynSearchArgs with IEsTypes {

  override def esTypes: Seq[String] = Nil

  override def toEsQueryOpt: Option[QueryBuilder] = {
    val qbOpt0 = super.toEsQueryOpt

    val _esTypes = esTypes
    if (_esTypes.isEmpty) {
      qbOpt0
    } else {
      val typeQb = QueryBuilders.termsQuery( SioEsUtil.StandardFieldNames.TYPE, _esTypes: _* )

      val qb2 = qbOpt0
        .fold[QueryBuilder]( typeQb ) { qb0 =>
          QueryBuilders
            .boolQuery()
            .must( qb0 )
            .filter( typeQb )
        }

      Some(qb2)
    }
  }

  override def sbInitSize: Int =
    collStringSize( esTypes, super.sbInitSize )

  override def toStringBuilder: StringBuilder =
    fmtColl2sb(  "esTypes", esTypes, super.toStringBuilder )

}
