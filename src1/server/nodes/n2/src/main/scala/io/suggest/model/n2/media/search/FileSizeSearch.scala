package io.suggest.model.n2.media.search

import io.suggest.es.model.{IMust, MWrapClause}
import io.suggest.es.search.DynSearchArgs
import io.suggest.model.n2.media.MMediaFields
import org.elasticsearch.index.query.{QueryBuilder, QueryBuilders}

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 03.10.17 13:40
  * Description: Поиск по размеру файла.
  */
trait FileSizeSearch extends DynSearchArgs {

  /** Поиск по байтовому размеру файла (объединяющий OR). */
  def fileSizeB: Seq[Long]

  override def toEsQueryOpt: Option[QueryBuilder] = {
    val qbOpt0 = super.toEsQueryOpt
    val byteSizes = fileSizeB
    if (byteSizes.isEmpty) {
      qbOpt0
    } else {
      val fn = MMediaFields.FileMeta.FM_SIZE_B_FN
      // Задан размер файла для поиска.
      val bsClauses = for (byteSize <- byteSizes) yield {
        MWrapClause(
          must          = IMust.SHOULD,
          queryBuilder  = QueryBuilders.termQuery( fn, byteSize )
        )
      }
      val qb2 = IMust.maybeWrapToBool( bsClauses )

      qbOpt0
        .map { qb0 =>
          QueryBuilders.boolQuery()
            .must(qb0)
            .filter(qb2)
        }
        .orElse {
          Some( qb2 )
        }
    }
  }

}


/** Дефолтовая реализация полей [[FileSizeSearch]]. */
trait FileSizeSearchDflt extends FileSizeSearch {

  override def fileSizeB: Seq[Long] = Nil

}
