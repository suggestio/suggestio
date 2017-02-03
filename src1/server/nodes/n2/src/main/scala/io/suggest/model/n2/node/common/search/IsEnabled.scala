package io.suggest.model.n2.node.common.search

import io.suggest.es.search.{DynSearchArgs, DynSearchArgsWrapper}
import io.suggest.model.n2.node.MNodeFields
import org.elasticsearch.index.query.{QueryBuilder, QueryBuilders}

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
    isEnabled.fold(qbOpt0) { _isEnabled =>
      val fn = MNodeFields.Common.IS_ENABLED_FN
      val isEnabledQ = QueryBuilders.termQuery(fn, _isEnabled)
      qbOpt0.map { qb =>
        QueryBuilders.boolQuery()
          .must(qb)
          .filter(isEnabledQ)
      }.orElse {
        Some(isEnabledQ)
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
