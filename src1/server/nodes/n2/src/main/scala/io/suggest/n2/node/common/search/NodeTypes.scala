package io.suggest.n2.node.common.search

import io.suggest.es.search.DynSearchArgs
import io.suggest.n2.node.{MNodeFields, MNodeType}
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
  def nodeTypes: Seq[MNodeType] = Nil

  override def toEsQueryOpt: Option[QueryBuilder] = {
    val qbOpt0 = super.toEsQueryOpt
    val _nodeTypes = nodeTypes
    if (_nodeTypes.isEmpty) {
      qbOpt0
    } else {
      val fn = MNodeFields.Common.NODE_TYPE_FN
      val strNodeTypes = _nodeTypes.map(_.value)
      val ntq = QueryBuilders.termsQuery(fn, strNodeTypes: _*)
      qbOpt0
        .map { qb0 =>
          QueryBuilders.boolQuery()
            .must(qb0)
            .filter(ntq)
        }
        .orElse {
          Some(ntq)
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
