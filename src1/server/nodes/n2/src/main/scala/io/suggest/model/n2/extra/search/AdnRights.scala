package io.suggest.model.n2.extra.search

import io.suggest.adn.MAdnRight
import io.suggest.es.model.{IMust, MWrapClause}
import io.suggest.es.search.{DynSearchArgs, DynSearchArgsWrapper}
import io.suggest.model.n2.node.MNodeFields
import org.elasticsearch.index.query.{QueryBuilder, QueryBuilders}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 06.10.15 14:51
 * Description: Аддон для поиска по полю MNode.extra.adn.rigths.
 */
trait AdnRights extends DynSearchArgs {

  /** Права, которые должны быть у узла. */
  def withAdnRights: Seq[MAdnRight]

  /** Вместо must использовать mustNot по отношению к заданным withAdnRights. */
  def adnRightsMustOrNot: Boolean

  /** Сборка EsQuery сверху вниз. */
  override def toEsQueryOpt: Option[QueryBuilder] = {
    val qbOpt0 = super.toEsQueryOpt
    val _withAdnRights = withAdnRights
    if (_withAdnRights.isEmpty) {
      qbOpt0

    } else {
      val fn = MNodeFields.Extras.ADN_RIGHTS_FN

      val mustOrNot = IMust.mustOrNot(adnRightsMustOrNot)
      // Собираем terms query, объединяя через AND (must).
      val allTermsQ = IMust.maybeWrapToBool {
        for (r <- _withAdnRights) yield {
          MWrapClause(mustOrNot, QueryBuilders.termQuery(fn, r.value))
        }
      }

      // Накатить собранную termsQuery на исходную query.
      qbOpt0
        .map { qb =>
          QueryBuilders.boolQuery()
            .must(qb)
            .filter(allTermsQ)
        }
        .orElse {
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
  override def withAdnRights: Seq[MAdnRight] = Seq.empty
  override def adnRightsMustOrNot: Boolean = true
}


/** Wrap-реализация [[AdnRights]]. */
trait AdnRightsWrap extends AdnRights with DynSearchArgsWrapper {
  override type WT <: AdnRights
  override def withAdnRights = _dsArgsUnderlying.withAdnRights
}
