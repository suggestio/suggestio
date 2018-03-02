package io.suggest.model.n2.media.search

import io.suggest.common.empty.EmptyProduct
import io.suggest.crypto.hash.MHash
import io.suggest.es.model.{IMust, MWrapClause, Must_t}
import io.suggest.es.search.DynSearchArgs
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
      val hashesTypeFn = F.HASHES_TYPE_FN
      val hashesValueFn = F.HASHES_VALUE_FN
      val nestedPath = F.HASHES_FN

      val crQbsIter = for (cr <- fhhIter) yield {
        // Сборка одной query по одному критерию (внутри nested).
        val qb = QueryBuilders.boolQuery()

        // TODO Возможно, тут ошибка: все одновременно хэши быть не могут ведь? Надо сделать по аналогии с NodeIdSearch, где через пачку SHOULD сделано.
        if (cr.hTypes.nonEmpty) {
          qb.must(
            QueryBuilders.termsQuery( hashesTypeFn, cr.hTypes.map(_.value): _* )
          )
        }

        if (cr.hexValues.nonEmpty) {
          qb.must(
            QueryBuilders.termsQuery( hashesValueFn, cr.hexValues: _* )
          )
        }

        val qbNest = QueryBuilders.nestedQuery( nestedPath, qb, ScoreMode.Max )

        MWrapClause(
          must          = cr.must,
          queryBuilder  = qbNest
        )
      }
      val crQbs = crQbsIter.toSeq

      // Объеденить все qb как must.
      val allCrsQb = IMust.maybeWrapToBool( crQbs )

      // Сборка итоговой query
      qbOpt0.map { qb0 =>
        QueryBuilders.boolQuery()
          .must( qb0 )
          .must( allCrsQb )
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


/** Критерий поиска по hash value.
  *
  * @param hTypes Искомые типы хешей.
  * @param hexValues Искомые значение хешей.
  */
case class MHashCriteria(
                              hTypes     : Seq[MHash]    = Nil,
                              hexValues  : Seq[String]   = Nil,
                              must       : Must_t        = IMust.MUST
                            )
  extends EmptyProduct

