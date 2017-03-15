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

  implicit object MTreeFastEq extends FastEq[MTree] {
    override def eqv(a: MTree, b: MTree): Boolean = {
      (a.nodes eq b.nodes) &&
        (a.showProps eq b.showProps)
    }
  }

}


/** Класс модели состояния дерева узлов.
  *
  * @param nodes Дерево узлов, скомпиленное на основе данных сервера.
  * @param showProps Фокусированный элемент дерева. Он только один. Для него отображаются элементы управления.
  */
case class MTree(
                  nodes       : Seq[MNodeState],
                  showProps   : Option[RcvrKey] = None
                )
{

  def withNodes(nodes2: Seq[MNodeState]) = copy(nodes = nodes2)
  def withShowProps(showProps2: Option[RcvrKey]) = copy(showProps = showProps2)

}
