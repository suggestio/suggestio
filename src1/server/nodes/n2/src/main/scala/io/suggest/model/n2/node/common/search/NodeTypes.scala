package io.suggest.model.n2.node.common.search

import io.suggest.es.search.{DynSearchArgs, DynSearchArgsWrapper}
import io.suggest.model.n2.node.{MNodeFields, MNodeType}
import org.elasticsearch.index.query.{QueryBuilder, QueryBuilders}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 05.10.15 17:12
 * Description: Аддон для поиска по типам узлов.
 */
trait NodeTypes extends DynSearchArgs {

  /** Искомые типы узлов.
    * Если Seq(null), то будет поиск документов без сохранного значения. */
  def nodeTypes: Seq[MNodeType]

  override def toEsQueryOpt: Option[QueryBuilder] = {
    val qbOpt0 = super.toEsQueryOpt
    val _nodeTypes = nodeTypes
    if (_nodeTypes.isEmpty) {
      qbOpt0
    } else {
      val fn = MNodeFields.Common.NODE_TYPE_FN
      if (nodeTypes.head == null) {
        // Seq(null) -- режим поиска элементов, у которых отсутствует сохранянное значение в поле ntype.
        val qb0 = qbOpt0 getOrElse {
          QueryBuilders.matchAllQuery()
        }
        val qb1 = QueryBuilders.boolQuery()
          .must(qb0)
          .mustNot( QueryBuilders.existsQuery( fn ) )
        Some(qb1)

      } else {
        val strNodeTypes = _nodeTypes.map(_.value)
        val ntq = QueryBuilders.termsQuery(fn, strNodeTypes: _*)
        qbOpt0.map { qb0 =>
          QueryBuilders.boolQuery()
            .must(qb0)
            .filter(ntq)
        }.orElse {
          Some(ntq)
        }
      }
    }
  }

  /** Базовый размер StringBuilder'а. */
  override def sbInitSize: Int = {
    val sz0 = super.sbInitSize
    val _nt = nodeTypes
    if (nodeTypes.isEmpty) {
      sz0
    } else {
      sz0 + 18 + _nt.length*3
    }
  }

  /** Построение выхлопа метода toString(). */
  override def toStringBuilder: StringBuilder = {
    fmtColl2sb("nodeTypes", nodeTypes, super.toStringBuilder)
  }

}


/** Дефолтовая реализация поисковых полей [[NodeTypes]] */
trait NodeTypesDflt extends NodeTypes {
  override def nodeTypes: Seq[MNodeType] = Nil
}


/** Wrap-реализация поисковых полей [[NodeTypes]]. */
trait NodeTypesWrap extends NodeTypes with DynSearchArgsWrapper {
  override type WT <: NodeTypes
  override def nodeTypes = _dsArgsUnderlying.nodeTypes
}
