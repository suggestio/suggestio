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
        (a.focused eq b.focused) &&
        (a.currHdr eq b.currHdr)
    }
  }

}


/** Класс модели состояния дерева узлов.
  *
  * @param nodes Дерево узлов, скомпиленное на основе данных сервера.
  * @param focused Фокусированный элемент дерева. Он только один. Для него отображаются элементы управления.
  * @param currHdr Заголовок вот этого узла помечен как текущий.
  *                Т.е. мышька сейчас наведена на заголовок указанного узла.
  */
case class MTree(
                  nodes       : Seq[MNodeState],
                  focused     : Option[RcvrKey] = None,
                  currHdr     : Option[RcvrKey] = None
                )
{

  def withNodes(nodes2: Seq[MNodeState]) = copy(nodes = nodes2)
  def withFocused(focused2: Option[RcvrKey]) = copy(focused = focused2)
  def withCurrHdr(currHdr2: Option[RcvrKey]) = copy(currHdr = currHdr2)

}
