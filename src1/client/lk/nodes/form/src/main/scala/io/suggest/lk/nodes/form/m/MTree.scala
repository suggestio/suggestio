package io.suggest.lk.nodes.form.m

import diode.FastEq
import io.suggest.adv.rcvr.RcvrKey
import japgolly.univeq.UnivEq

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 08.02.17 18:20
  * Description: Модель состояния дерева узлов и связанных с ним вещей.
  */
object MTree {

  implicit object MTreeFastEq extends FastEq[MTree] {
    override def eqv(a: MTree, b: MTree): Boolean = {
      (a.nodes eq b.nodes) &&
        (a.showProps eq b.showProps)
    }
  }

  implicit def univEq: UnivEq[MTree] = {
    import io.suggest.ueq.UnivEqUtil._
    UnivEq.derive
  }

}


/** Класс модели состояния дерева узлов.
  *
  * @param nodes Дерево узлов, скомпиленное на основе данных сервера.
  * @param showProps rcvr-key для узла дерева, у которого сейчас отображаются полный список properties'ов.
  */
case class MTree(
                  nodes       : Seq[MNodeState],
                  showProps   : Option[RcvrKey] = None
                )
{

  def withNodes(nodes2: Seq[MNodeState]) = copy(nodes = nodes2)
  def withShowProps(showProps2: Option[RcvrKey]) = copy(showProps = showProps2)

}
