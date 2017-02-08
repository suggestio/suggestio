package io.suggest.lk.nodes.form.m

import diode.FastEq
import io.suggest.lk.nodes.MLknForm

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 08.02.17 16:11
  * Description: Корневая circuit-модель react-формы управления под-узлами.
  */
object MLkNodesRoot {

  object MLknRootFastEq extends FastEq[MLkNodesRoot] {
    override def eqv(a: MLkNodesRoot, b: MLkNodesRoot): Boolean = {
      a.tree eq b.tree
    }
  }

}


case class MLkNodesRoot(
                         tree: MTree
                       )
{

  def toForm: MLknForm = ???

}
