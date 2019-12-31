package io.suggest.model.n2.media.search

import io.suggest.es.model.{IMust, MWrapClause}
import io.suggest.es.search.DynSearchArgs
import io.suggest.model.n2.edge.search.MHashCriteria
import io.suggest.model.n2.media.MMediaFields
import org.apache.lucene.search.join.ScoreMode
import org.elasticsearch.index.query.{QueryBuilder, QueryBuilders}

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 02.10.17 22:42
  * Description: Поиск по хешам файла.
  */
trait FileHashSearch extends DynSearchArgs {

  /** Хеш-суммы файла, объединяются через AND. */
  def fileHashesHex: Seq[MHashCriteria]

  /** Сборка EsQuery сверху вниз. */
  override def toEsQueryOpt: Option[QueryBuilder] = {
    val qbOpt0 = super.toEsQueryOpt

    val fhhIter = fileHashesHex
      .iterator
      .filter(_.nonEmpty)

    if (fhhIter.isEmpty) {
      qbOpt0

    } else {
      // Заданы хэш-суммы искомого файла. TODO Подготовить матчинг. Тут у нас nested search требуется..
      val F = MMediaFields.FileMeta
      lazy val hashesTypeFn = F.FM_HASHES_TYPE_FN
      lazy val hashesValueFn = F.FM_HASHES_VALUE_FN
      lazy val nestedPath = F.FM_HASHES_FN

      val crQbs = (for (cr <- fhhIter) yield {
        // Сборка одной query по одному критерию (внутри nested).
        val qb = QueryBuilders.boolQuery()

        // TODO Возможно, тут ошибка: все одновременно хэши быть не могут ведь? Надо сделать по аналогии с NodeIdSearch, где через пачку SHOULD сделано.
        if (cr.hTypes.nonEmpty) {
          val hTypesQb = QueryBuilders.termsQuery( hashesTypeFn, cr.hTypes.map(_.value): _* )
          qb.filter( hTypesQb )
        }

        if (cr.hexValues.nonEmpty) {
          val hValuesQb = QueryBuilders.termsQuery( hashesValueFn, cr.hexValues: _* )
          qb.filter( hValuesQb )
        }

        val qbNest = QueryBuilders.nestedQuery( nestedPath, qb, ScoreMode.None )

        MWrapClause(
          must          = cr.must,
          queryBuilder  = qbNest
        )
      })
        .toSeq

      // Объеденить все qb как must.
      val allCrsQb = IMust.maybeWrapToBool( crQbs )

      // Сборка итоговой query
      qbOpt0.map { qb0 =>
        QueryBuilders.boolQuery()
          .must( qb0 )
          .filter( allCrsQb )
      }.orElse {
        Some( allCrsQb )
      }
    }
  }

}


/** Дефолтовая реализация [[FileHashSearch]]. */
trait FileHashSearchDflt extends FileHashSearch {
  override def fileHashesHex: Seq[MHashCriteria] = Nil
}

