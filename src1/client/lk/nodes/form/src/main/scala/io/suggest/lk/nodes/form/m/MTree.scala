package io.suggest.lk.nodes.form.m

import diode.FastEq
import io.suggest.adv.rcvr.RcvrKey
import japgolly.univeq.UnivEq
import monocle.macros.GenLens

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

  @inline implicit def univEq: UnivEq[MTree] = {
    import io.suggest.ueq.UnivEqUtil._
    UnivEq.derive
  }

  def nodes = GenLens[MTree](_.nodes)
  val showProps = GenLens[MTree](_.showProps)

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
