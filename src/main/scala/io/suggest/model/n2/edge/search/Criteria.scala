package io.suggest.model.n2.edge.search

import io.suggest.common.{IEmpty, EmptyProduct}
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
  def nodeIds     : Seq[String]
  def predicates  : Seq[String]
  def sls         : Seq[SlNameTokenStr]
}

case class Criteria(
  nodeIds     : Seq[String] = Nil,
  predicates  : Seq[String] = Nil,
  sls         : Seq[SlNameTokenStr] = Nil
)
  extends ICriteria
  with EmptyProduct

