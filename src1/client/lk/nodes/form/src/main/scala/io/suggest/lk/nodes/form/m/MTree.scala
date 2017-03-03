package io.suggest.lk.nodes.form.m

import diode.FastEq

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 08.02.17 18:20
  * Description: Модель состояния дерева узлов и связанных с ним вещей.
  */
object MTree {

  object MTreeFastEq extends FastEq[MTree] {
    override def eqv(a: MTree, b: MTree): Boolean = {
      a.nodes eq b.nodes
    }
  }

}


case class MTree(
                  nodes: Seq[MNodeState]
                )
