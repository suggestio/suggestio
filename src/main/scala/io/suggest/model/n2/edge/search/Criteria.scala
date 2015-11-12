package io.suggest.model.n2.edge.search

import io.suggest.common.{IEmpty, EmptyProduct}
import io.suggest.model.n2.edge.MPredicate
import io.suggest.ym.model.common.SlNameTokenStr

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 02.10.15 14:42
 * Description: Критерий для поиска по node-edges.
 * Заданные здесь множества id-узлов и их предикатов проверяются в рамках каждого nested-объкета.
 * Для задания нескольких критериев поиска нужно несколько критериев перечислить.
 */

trait ICriteria extends IEmpty {

  /** id искомых узлов. */
  def nodeIds     : Seq[String]

  /** id предикатов. */
  def predicates  : Seq[MPredicate]

  /** Искомые уровни отображения. */
  def sls         : Seq[SlNameTokenStr]

  /** Флаг наличия любого уровня или отсутствия всех уровней отображения. */
  def anySl       : Option[Boolean]

  /**
   * Вместо should clause будет использована must или mustNot для true или false соответственно.
   * Т.е. тут можно управлять семантикой объединения нескольких критериев, как если бы [OR, AND, NAND].
   * @return None для should. Хотя бы один из should-clause всегда должен быть истинным.
   *         Some(true) -- обязательный clause, должна обязательно быть истинной.
   *         Some(false) -- негативный clause, т.е. срабатывания выкидываются из выборки результатов.
   */
  def must        : Option[Boolean]

  /** Состояние дополнительного флага в контейнера info. */
  def flag        : Option[Boolean]


  override def toString: String = {
    getClass.getSimpleName + "(" + nodeIds + "," + predicates + "," + sls + "," + anySl + ")"
  }

}


/** Дефолтовая реализация [[ICriteria]]. */
case class Criteria(
  override val nodeIds     : Seq[String]          = Nil,
  override val predicates  : Seq[MPredicate]      = Nil,
  override val sls         : Seq[SlNameTokenStr]  = Nil,
  override val anySl       : Option[Boolean]      = None,
  override val must        : Option[Boolean]      = None,
  override val flag        : Option[Boolean]      = None
)
  extends ICriteria
  with EmptyProduct

