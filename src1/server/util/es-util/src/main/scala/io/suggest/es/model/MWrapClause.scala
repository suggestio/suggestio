package io.suggest.es.model

import org.elasticsearch.index.query.{QueryBuilder, QueryBuilders}

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 07.07.2020 16:57
  * Description: Обёртка на QueryBuilder, чтобы пробросить доп.данные [[IMust]]/[[Must_t]].
  */
object MWrapClause {

  /**
    * Сборка списка скомпиленных запросов в один запрос.
    * Обычно происходит заворачивание в bool query.
    *
    * @param clauses Входящий список критериев и готовых QueryBuilder'ов.
    * @return Итоговый QueryBuilder.
    */
  def maybeWrapToBool(clauses: Iterable[MWrapClause]): QueryBuilder = {
    if ((clauses.sizeIs > 1) || clauses.exists(_.must contains[Boolean] false)) {
      // Возврат значения происходит через закидывание сгенеренной query в BoolQuery.
      var shouldClauses = 0
      val nq = QueryBuilders.boolQuery()

      for (c <- clauses) {
        // Клиент может настраивать запрос с помощью must/should/mustNot.
        val qb = c.queryBuilder
        c.must.fold [Unit] {
          nq.should( qb )
          shouldClauses += 1
        } {
          case true =>
            nq.must( qb )
          case _ =>
            nq.mustNot( qb )
        }
      }
      // Если should-clause'ы отсутствуют, то minimum should match 0. Иначе 1.
      nq.minimumShouldMatch(
        Math.min(1, shouldClauses)
      )

    } else {
      clauses.head.queryBuilder
    }
  }


  implicit final class MwcsOpsExt( private val mwcs: Iterable[MWrapClause] ) extends AnyVal {
    def toBoolQuery = maybeWrapToBool( mwcs )
  }

}


/** Один search query builder в связке с [[IMust]]. */
case class MWrapClause(
                        override val must         : Must_t,
                        queryBuilder              : QueryBuilder
                      )
  extends IMust
