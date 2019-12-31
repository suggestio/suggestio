package io.suggest.model.n2.media.search

import io.suggest.es.search.DynSearchArgs
import io.suggest.model.n2.media.MMediaFields
import org.elasticsearch.index.query.{QueryBuilder, QueryBuilders}

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 03.10.17 13:50
  * Description: Поиск по флагу isOriginal.
  */
trait IsOriginalFileSearch extends DynSearchArgs {

  def isOriginalFile: Option[Boolean]

  /** Сборка EsQuery сверху вниз. */
  override def toEsQueryOpt: Option[QueryBuilder] = {
    val qbOpt0 = super.toEsQueryOpt
    isOriginalFile.fold(qbOpt0) { isOrig =>
      // Есть искомое значение флага. Собираем фильтр:
      val isOrigQb = QueryBuilders.termQuery( MMediaFields.FileMeta.FM_IS_ORIGINAL_FN, isOrig )
      qbOpt0
        .map { qb0 =>
          QueryBuilders.boolQuery()
            .must( qb0 )
            .filter( isOrigQb )
        }
        .orElse {
          Some(isOrigQb)
        }
    }
  }

}


/** Дефолтовая реализация полей из [[IsOriginalFileSearch]]. */
trait IsOriginalFileSearchDflt extends IsOriginalFileSearch {
  override def isOriginalFile: Option[Boolean] = None
}
