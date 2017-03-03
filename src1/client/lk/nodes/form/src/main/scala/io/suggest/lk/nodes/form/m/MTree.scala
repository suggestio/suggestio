package io.suggest.lk.nodes.form.m

import diode.FastEq
import io.suggest.adv.rcvr.RcvrKey

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 08.02.17 18:20
  * Description: Модель состояния дерева узлов и связанных с ним вещей.
  */
object MTree {

  object MTreeFastEq extends FastEq[MTree] {
    override def eqv(a: MTree, b: MTree): Boolean = {
      (a.nodes eq b.nodes) &&
        (a.addStates eq b.addStates)
    }
  }

}


case class MTree(
                  /** Дерево узлов, скомпиленное на основе данных сервера. */
                  nodes       : Seq[MNodeState],
                  /** id родительского узла (вкл.Nil) -> состояние формы добавления. */
                  addStates   : Map[RcvrKey, MAddSubNodeState] = Map.empty
                )
{

  def withNodes(nodes2: Seq[MNodeState]) = copy(nodes = nodes2)

  def withAddStates(addStates2: Map[RcvrKey, MAddSubNodeState]) = copy(addStates = addStates2)

}
