package io.suggest.es.model

import io.suggest.common.empty.OptionUtil
import org.elasticsearch.index.query.{QueryBuilder, QueryBuilders}

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 31.03.16 18:20
  * Description: Утиль для сборки запросов к ES.
  */

object IMust {

  // Множество значений поля must.
  def SHOULD    : Must_t  = None
  def MUST      : Must_t  = OptionUtil.SomeBool.someTrue
  def MUST_NOT  : Must_t  = OptionUtil.SomeBool.someFalse


  /** Вернуть MUST или MUST_NOT в зависимости от значения флага.
    *
    * @param flag true обязательно должен быть.
    *             false обязательно быть не должно.
    * @return Must_t.
    */
  def mustOrNot(flag: Boolean): Must_t = {
    // Оптимизация: вместо if-else используем упрощённый вариант:
    Some(flag)
  }

  def mustOrShould(flag: Boolean): Must_t = {
    if (flag) MUST else SHOULD
  }

  /**
    * Сборка списка скомпиленных запросов в один запрос.
    * Обычно происходит заворачивание в bool query.
    *
    * @param clauses Входящий список критериев и готовых QueryBuilder'ов.
    * @return Итоговый QueryBuilder.
    */
  def maybeWrapToBool(clauses: Iterable[MWrapClause]): QueryBuilder = {
    if ((clauses.sizeIs > 1) || clauses.exists(_.must contains false)) {
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

  def toString(must: Must_t): String = {
    must.fold ("should") {
      case true   => "must"
      case false  => "mustNot"
    }
  }

}


/** Один search query builder в связке с [[IMust]]. */
case class MWrapClause(
  override val must         : Must_t,
  queryBuilder              : QueryBuilder
)
  extends IMust


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
  def must: Must_t

}

