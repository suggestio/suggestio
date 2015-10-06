package io.suggest.model.n2.extra.search

import io.suggest.model.n2.node.MNode
import io.suggest.model.search.{DynSearchArgs, DynSearchArgsWrapper}
import org.elasticsearch.index.query.{FilterBuilders, QueryBuilder, QueryBuilders}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 06.10.15 15:52
 * Description: Поисковый аддон для поиска/фильтрации по флагу отображения в списке узлов выдачи.
 */
trait ShowInScNl extends DynSearchArgs {

  /** искать/фильтровать по флагу отображения в списке узлов поисковой выдачи. */
  def showInScNodeList: Option[Boolean]

  /** Сборка EsQuery сверху вниз. */
  override def toEsQueryOpt: Option[QueryBuilder] = {
    val qbOpt0 = super.toEsQueryOpt
    val _sscNlOpt = showInScNodeList
    if (_sscNlOpt.isEmpty) {
      qbOpt0

    } else {
      val fn = MNode.Fields.Extras.ADN_SHOW_IN_SC_NL_FN
      val _sscNl = _sscNlOpt.get
      qbOpt0 map { qb =>
        // Отрабатываем флаг conf.showInScNodeList
        val sscf = FilterBuilders.termFilter(fn, _sscNl)
        QueryBuilders.filteredQuery(qb, sscf)

      } orElse {
        val qb = QueryBuilders.termQuery(fn, _sscNl)
        Some(qb)
      }
    }
  }

  /** Базовый размер StringBuilder'а. */
  override def sbInitSize: Int = {
    val sis = super.sbInitSize
    if (showInScNodeList.isDefined)  sis + 24  else  sis
  }

  /** Построение выхлопа метода toString(). */
  override def toStringBuilder: StringBuilder = {
    fmtColl2sb("showInScNl", showInScNodeList, super.toStringBuilder)
  }
}


/** Дефолтовая реализация [[ShowInScNl]]. */
trait ShowInScNlDflt extends ShowInScNl {
  override def showInScNodeList: Option[Boolean] = None
}


/** Wrap-реализация [[ShowInScNl]]. */
trait ShowInScNlWrap extends ShowInScNl with DynSearchArgsWrapper {
  override type WT <: ShowInScNl
  override def showInScNodeList = _dsArgsUnderlying.showInScNodeList
}
