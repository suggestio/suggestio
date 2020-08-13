package io.suggest.lk.nodes.form.m

import diode.FastEq
import io.suggest.scalaz.NodePath_t
import japgolly.univeq._
import io.suggest.ueq.UnivEqUtil._
import monocle.macros.GenLens
import scalaz.Tree
import io.suggest.scalaz.ZTreeUtil._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 08.02.17 18:20
  * Description: Модель состояния дерева узлов и связанных с ним вещей.
  */
object MTree {

  implicit object MTreeFastEq extends FastEq[MTree] {
    override def eqv(a: MTree, b: MTree): Boolean = {
      (a.nodes ===* b.nodes) &&
      (a.opened ===* b.opened)
    }
  }

  @inline implicit def univEq: UnivEq[MTree] = {
    import io.suggest.ueq.UnivEqUtil._
    UnivEq.derive
  }

  def nodes = GenLens[MTree](_.nodes)
  val opened = GenLens[MTree](_.opened)

}


/** Класс модели состояния дерева узлов.
  *
  * @param nodes Дерево узлов, скомпиленное на основе данных сервера.
  * @param opened rcvr-key для узла дерева, у которого сейчас отображаются полный список properties'ов.
  */
case class MTree(
                  nodes       : Tree[MNodeState],
                  opened      : Option[NodePath_t] = None
                ) {

  lazy val openedLoc = opened.flatMap( nodes.loc.pathToNode )
  lazy val openedRcvrKey = openedLoc.map(_.rcvrKey)

}
