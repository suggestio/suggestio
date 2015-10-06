package io.suggest.model.n2.node.common.search

import io.suggest.model.n2.node.MNode
import io.suggest.model.search.{DynSearchArgsWrapper, DynSearchArgs}
import org.elasticsearch.index.query.{QueryBuilders, FilterBuilders, QueryBuilder}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 06.10.15 15:07
 * Description: Поиск/фильтрация по флагу MNode.extra.adn.isEnabled.
 */
trait IsEnabled extends DynSearchArgs {

  /** Искать/фильтровать по галочки активности узла. */
  def isEnabled: Option[Boolean]

  /** Сборка EsQuery сверху вниз. */
  override def toEsQueryOpt: Option[QueryBuilder] = {
    val qbOpt0 = super.toEsQueryOpt
    val _isOpt = isEnabled
    if (_isOpt.isEmpty) {
      qbOpt0

    } else {
      val fn = MNode.Fields.Common.IS_ENABLED_FN
      val _is = _isOpt.get
      qbOpt0 map { qb =>
        val ief = FilterBuilders.termFilter(fn, _is)
        QueryBuilders.filteredQuery(qb, ief)

      } orElse {
        val qb = QueryBuilders.termQuery(fn, isEnabled)
        Some(qb)
      }
    }
  }

  /** Базовый размер StringBuilder'а. */
  override def sbInitSize: Int = {
    val sis = super.sbInitSize
    if (isEnabled.isDefined) sis + 16 else sis
  }

  /** Построение выхлопа метода toString(). */
  override def toStringBuilder: StringBuilder = {
    fmtColl2sb("isEnabled", isEnabled, super.toStringBuilder)
  }

}


/** Дефолтовая реализция аддона [[IsEnabled]]. */
trait IsEnabledDflt extends IsEnabled {
  override def isEnabled: Option[Boolean] = None
}


/** Wrap-реализация аддона [[IsEnabled]]. */
trait IsEnabledWrap extends IsEnabled with DynSearchArgsWrapper {
  override type WT <: IsEnabled
  override def isEnabled = _dsArgsUnderlying.isEnabled
}
