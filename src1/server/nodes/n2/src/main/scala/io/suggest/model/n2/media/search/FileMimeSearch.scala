package io.suggest.model.n2.media.search

import io.suggest.es.model.{IMust, MWrapClause}
import io.suggest.es.search.DynSearchArgs
import io.suggest.model.n2.media.MMediaFields
import org.elasticsearch.index.query.{QueryBuilder, QueryBuilders}

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 02.03.18 15:49
  * Description: Поддержка поиска/фильтрации по mime-типу в MMedia.
  */
trait FileMimeSearch extends DynSearchArgs {

  def fileMimes: Seq[String]

  /** Сборка EsQuery сверху вниз. */
  override def toEsQueryOpt: Option[QueryBuilder] = {
    val qbOpt0 = super.toEsQueryOpt

    val mimes = fileMimes
    if (mimes.isEmpty) {
      qbOpt0

    } else {
      // Указаны допустимые mime-типы, значит будем фильтровать:
      val fn = MMediaFields.FileMeta.FM_MIME_FN

      // Т.к. нам нужен любой из списка допустимых mime-типов, надо делать пачку SHOULD clause:
      val nodeIdsWraps = for (mime <- mimes) yield {
        MWrapClause(
          must = IMust.SHOULD,
          queryBuilder = QueryBuilders.termQuery( fn, mime )
        )
      }

      val mimesQb = IMust.maybeWrapToBool( nodeIdsWraps )

      qbOpt0
        .map { qb0 =>
          QueryBuilders.boolQuery()
            .must( qb0 )
            .filter( mimesQb )
        }
        .orElse {
          Some( mimesQb )
        }
    }
  }

}


trait FileMimeSearchDflt extends FileMimeSearch {
  override def fileMimes: Seq[String] = Nil
}
