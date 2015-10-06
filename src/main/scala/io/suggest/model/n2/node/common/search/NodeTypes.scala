package io.suggest.model.n2.node.common.search

import io.suggest.model.n2.node.{MNode, MNodeType}
import io.suggest.model.search.{DynSearchArgs, DynSearchArgsWrapper}
import org.elasticsearch.index.query.{FilterBuilders, QueryBuilder, QueryBuilders}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 05.10.15 17:12
 * Description: Аддон для поиска по типам узлов.
 */
trait NodeTypes extends DynSearchArgs {

  /** Искомые типы узлов. */
  def nodeTypes: Seq[MNodeType]

  override def toEsQueryOpt: Option[QueryBuilder] = {
    val qbOpt0 = super.toEsQueryOpt
    val _nodeTypes = nodeTypes
    if (_nodeTypes.isEmpty) {
      qbOpt0
    } else {
      val fn = MNode.Fields.Common.NODE_TYPE_FN
      val strNodeTypes = _nodeTypes.map(_.strId)
      qbOpt0.map { qb0 =>
        val ntf = FilterBuilders.termsFilter(fn, strNodeTypes: _*)
          .execution("or")
        QueryBuilders.filteredQuery(qb0, ntf)

      }.orElse {
        val ntq = QueryBuilders.termsQuery(fn, strNodeTypes: _*)
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


/** Дефолтовая реализация поисковых полей [[NodeTypes]] */
trait NodeTypesDflt extends NodeTypes {
  override def nodeTypes: Seq[MNodeType] = Nil
}


/** Wrap-реализация поисковых полей [[NodeTypes]]. */
trait NodeTypesWrap extends NodeTypes with DynSearchArgsWrapper {
  override type WT <: NodeTypes
  override def nodeTypes = _dsArgsUnderlying.nodeTypes
}
