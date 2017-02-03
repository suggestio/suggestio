package io.suggest.model.n2.extra.search

import io.suggest.es.search.{DynSearchArgs, DynSearchArgsWrapper}
import io.suggest.model.n2.node.MNodeFields
import io.suggest.ym.model.common.AdnRight
import org.elasticsearch.index.query.{QueryBuilder, QueryBuilders}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 06.10.15 14:51
 * Description: Аддон для поиска по полю MNode.extra.adn.rigths.
 */
trait AdnRights extends DynSearchArgs {

  /** Права, которые должны быть у узла. */
  def withAdnRights: Seq[AdnRight]

  /** Сборка EsQuery сверху вниз. */
  override def toEsQueryOpt: Option[QueryBuilder] = {
    val qbOpt0 = super.toEsQueryOpt
    val _war = withAdnRights
    if (_war.isEmpty) {
      qbOpt0
    } else {
      val fn = MNodeFields.Extras.ADN_RIGHTS_FN
      // Собираем termsQuery()
      val allTermsQ = QueryBuilders.boolQuery()
      for (name <- withAdnRights.map(_.name)) {
        val tq = QueryBuilders.termQuery(fn, name)
        allTermsQ.must(tq)
      }
      // Накатить собранную termsQuery на исходную query.
      qbOpt0.map { qb =>
        QueryBuilders.boolQuery()
          .must(qb)
          .filter(allTermsQ)
      }.orElse {
        Some(allTermsQ)
      }
    }
  }

  /** Базовый размер StringBuilder'а. */
  override def sbInitSize: Int = {
    collStringSize(withAdnRights, super.sbInitSize, addOffset = 10)
  }

  /** Построение выхлопа метода toString(). */
  override def toStringBuilder: StringBuilder = {
    fmtColl2sb("withAdnRights", withAdnRights, super.toStringBuilder)
  }

}


/** Дефолтовая реализация абстрактных кусков [[AdnRights]]. */
trait AdnRightsDflt extends AdnRights {
  override def withAdnRights: Seq[AdnRight] = Seq.empty
}


/** Wrap-реализация [[AdnRights]]. */
trait AdnRightsWrap extends AdnRights with DynSearchArgsWrapper {
  override type WT <: AdnRights
  override def withAdnRights = _dsArgsUnderlying.withAdnRights
}
