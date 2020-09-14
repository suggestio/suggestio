package io.suggest.lk.nodes.form.m

import diode.FastEq
import diode.data.Pot
import io.suggest.scalaz.NodePath_t
import japgolly.univeq._
import io.suggest.ueq.UnivEqUtil._
import monocle.macros.GenLens
import scalaz.{Tree, TreeLoc}
import io.suggest.scalaz.ZTreeUtil._
import io.suggest.ueq.JsUnivEqUtil._

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

  /** Короткий код выставления дерева в состояние. */
  def setNodes(tree2: Tree[MNodeState]) = nodes.modify(_.ready(tree2))

  /** Пустое дерево с корнем для инициализации поля MTree().nodes */
  def emptyNodesTreePot =
    Pot.empty.ready(
      Tree.Leaf(
        MNodeState.mkRoot ) )


  implicit final class TreeExt( private val mtree: MTree ) extends AnyVal {

    /** Короткий код доступа к указанной локации в дереве. */
    def pathToLoc(nodePath: NodePath_t): Option[TreeLoc[MNodeState]] = {
      mtree
        .nodes
        .toOption
        .flatMap( _.loc.pathToNode( nodePath ) )
    }

  }

}


/** Класс модели состояния дерева узлов.
  *
  * @param nodes Дерево узлов, скомпиленное на основе данных сервера.
  *              Pot, т.к. на момент запуска оно может быть в состоянии запроса начальных данных дерева с сервера.
  *              При дальнейших запросах частей дерева используются обычно под-состояния внутри ветвей (и могут быть параллельные запросы разных ветвей).
  * @param opened rcvr-key для узла дерева, у которого сейчас отображаются полный список properties'ов.
  */
case class MTree(
                  nodes       : Pot[Tree[MNodeState]],
                  opened      : Option[NodePath_t]      = None,
                ) {

  lazy val nodesOpt = nodes.toOption

  lazy val openedLoc: Option[TreeLoc[MNodeState]] =
    opened.flatMap( op =>
      nodesOpt.flatMap( nodesTree =>
        nodesTree.loc.pathToNode(op) ) )

  lazy val openedRcvrKey = openedLoc.map(_.rcvrKey)

}
