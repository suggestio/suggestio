package models.msys

import io.suggest.model.n2.node.MNode

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 18.06.15 17:01
 * Description: Модель выявленной проблемы с узлом при каком-то тесте.
 */
case class NodeProblem(
  mnode: MNode,
  ex   : Throwable
)
