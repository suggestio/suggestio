package io.suggest.model.es

import org.elasticsearch.index.query.{QueryBuilder, QueryBuilders}

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 31.03.16 18:20
  * Description: Утиль для сборки запросов к ES.
  */
object QueryUtil {

  /**
    * Сборка списка скомпиленных запросов в один запрос.
    * Обычно происходит заворачивание в bool query.
    *
    * @param clauses Входящий список критериев и готовых QueryBuilder'ов.
    * @return Итоговый QueryBuilder.
    */
  def maybeWrapToBool(clauses: Traversable[(IMust, QueryBuilder)]): QueryBuilder = {
    if (clauses.size > 1 || clauses.exists(_._1.must.contains(false)) ) {
      // Возврат значения происходит через закидывание сгенеренной query в BoolQuery.
      var shouldClauses = 0
      val nq = QueryBuilders.boolQuery()

      for ((oe, _q) <- clauses) {
        // Клиент может настраивать запрос с помощью must/should/mustNot.
        oe.must match {
          case None =>
            nq.should(_q)
            shouldClauses += 1
          case Some(true) =>
            nq.must(_q)
          case _ =>
            nq.mustNot(_q)
        }
      }
      // Если should-clause'ы отсутствуют, то minimum should match 0. Иначе 1.
      nq.minimumNumberShouldMatch(
        Math.min(1, shouldClauses)
      )

    } else {
      clauses.head._2
    }
  }

}


object IMust {
  def SHOULD: Option[Boolean]   = None
  def MUST                      = Some(true)
  def MUST_NOT                  = Some(false)
}


/** Интерфейс к must-полям, которые описывают выбор между should, must и mustNot. */
trait IMust {

  /**
   * Вместо should clause будет использована must или mustNot для true или false соответственно.
   * Т.е. тут можно управлять семантикой объединения нескольких критериев, как если бы [OR, AND, NAND].
   *
   * @return None для should. Хотя бы один из should-clause всегда должен быть истинным.
   *         Some(true) -- обязательный clause, должна обязательно быть истинной.
   *         Some(false) -- негативный clause, т.е. срабатывания выкидываются из выборки результатов.
   */
  def must: Option[Boolean]

}
