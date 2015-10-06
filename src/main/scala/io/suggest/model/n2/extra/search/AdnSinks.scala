package io.suggest.model.n2.extra.search

import io.suggest.model.n2.node.MNode
import io.suggest.model.search.{DynSearchArgsWrapper, DynSearchArgs}
import io.suggest.ym.model.common.AdnSink
import org.elasticsearch.index.query.{QueryBuilders, FilterBuilders, QueryBuilder}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 05.10.15 22:43
 * Description:
 */
trait AdnSinks extends DynSearchArgs {

  /** Искать/фильтровать по доступным sink'ам. */
  def onlyWithSinks: Seq[AdnSink]

  /** Сборка EsQuery сверху вниз. */
  override def toEsQueryOpt: Option[QueryBuilder] = {
    val qbOpt0 = super.toEsQueryOpt
    val _ows = onlyWithSinks
    if (_ows.isEmpty) {
      qbOpt0
    } else {
      val _owsStr = onlyWithSinks.map(_.name)
      // Отрабатываем возможный список прав узла.
      val fn = MNode.Fields.Extras.ADN_SINKS_FN
      qbOpt0.map { qb =>
        val sf = FilterBuilders.termsFilter(fn, _owsStr: _*)
        QueryBuilders.filteredQuery(qb, sf)

      }.orElse {
        val sq = QueryBuilders.termsQuery(fn, _owsStr: _*)
          .minimumMatch(onlyWithSinks.size)
        Some(sq)
      }
    }
  }

  /** Базовый размер StringBuilder'а. */
  override def sbInitSize: Int = {
    collStringSize(onlyWithSinks, super.sbInitSize, 5)
  }

  /** Построение выхлопа метода toString(). */
  override def toStringBuilder: StringBuilder = {
    fmtColl2sb("onlyWithSinks", onlyWithSinks, super.toStringBuilder)
  }

}


/** Дефолтовая реализация аддона [[AdnSinks]]. */
trait AdnSinksDflt extends AdnSinks {
  override def onlyWithSinks: Seq[AdnSink] = Nil
}


/** wrap-реализация для аддона и [[AdnSinks]]. */
trait AdnSinksWrap extends AdnSinks with DynSearchArgsWrapper {
  override type WT <: AdnSinks
  override def onlyWithSinks = _dsArgsUnderlying.onlyWithSinks
}
