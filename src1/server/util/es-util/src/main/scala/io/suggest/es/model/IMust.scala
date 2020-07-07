package io.suggest.es.model

import io.suggest.common.empty.OptionUtil

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

  def toString(must: Must_t): String = {
    must.fold ("should") {
      case true   => "must"
      case false  => "mustNot"
    }
  }

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
  def must: Must_t

}

