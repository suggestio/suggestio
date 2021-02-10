package io.suggest.lk.nodes.form.m

import diode.FastEq
import diode.data.Pot
import io.suggest.scalaz.NodePath_t
import io.suggest.scalaz.ScalazUtil.Implicits._
import japgolly.univeq._
import io.suggest.ueq.UnivEqUtil._
import monocle.macros.GenLens
import scalaz.{Tree, TreeLoc}
import io.suggest.scalaz.ZTreeUtil._
import io.suggest.ueq.JsUnivEqUtil._

import scala.collection.immutable.HashMap

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 08.02.17 18:20
  * Description: Модель состояния дерева узлов и связанных с ним вещей.
  */
object MTree {

  implicit object MTreeFastEq extends FastEq[MTree] {
    override def eqv(a: MTree, b: MTree): Boolean = {
      (a.idsTree ===* b.idsTree) &&
      (a.opened ===* b.opened) &&
      (a.nodesMap ===* b.nodesMap)
    }
  }

  @inline implicit def univEq: UnivEq[MTree] = {
    import io.suggest.ueq.UnivEqUtil._
    UnivEq.derive
  }

  def idsTree = GenLens[MTree](_.idsTree)
  val opened = GenLens[MTree](_.opened)
  def nodesMap = GenLens[MTree](_.nodesMap)

  /** Короткий код выставления дерева в состояние. */
  def setNodes(tree2: Tree[String]) = idsTree.modify(_.ready(tree2))

  /** Пустое дерево с корнем для инициализации поля MTree().nodes */
  def emptyNodesTree(isPending: Boolean = false) = {
    val rootId = MTreeRoles.Root.treeId

    var nodesTreePot = Pot.empty
      .ready( Tree.Leaf( rootId ) )
    if (isPending)
      nodesTreePot = nodesTreePot.pending()

    apply(
      idsTree    = nodesTreePot,
      nodesMap = HashMap.empty + (rootId -> MNodeState.mkRootNode),
    )
  }


  implicit final class TreeExt( private val mtree: MTree ) extends AnyVal {

    /** Короткий код доступа к указанной локации в дереве. */
    def pathToLoc(nodePath: NodePath_t): Option[TreeLoc[String]] = {
      mtree
        .idsTree
        .toOption
        .flatMap( _.loc.pathToNode( nodePath ) )
    }

  }

}


/** Класс модели состояния дерева узлов.
  *
  * @param idsTree Дерево id узлов, скомпиленное из данных сервера.
  *                Сами состояния узлов хранятся в поле nodesMap.
  *                Pot, т.к. на момент запуска оно может быть в состоянии запроса начальных данных дерева с сервера.
  *                При дальнейших запросах частей дерева используются обычно под-состояния внутри ветвей (и могут быть параллельные запросы разных ветвей).
  * @param opened rcvr-key для узла дерева, у которого сейчас отображаются полный список properties'ов.
  * @param nodesMap Карта инфы по узлам в дереве.
  *                 В самом дереве содержатся только id узлов в данной карте.
  * param reRenderTimer Таймер принудительного пере-рендера дерева.
  */
case class MTree(
                  idsTree             : Pot[Tree[String]],
                  opened              : Option[NodePath_t]                = None,
                  nodesMap            : HashMap[String, MNodeState],
                ) {

  lazy val idsTreeOpt = idsTree.toOption

  lazy val openedLoc: Option[TreeLoc[String]] = {
    for {
      op        <- opened
      nodesTree <- idsTreeOpt
      mtk       <- nodesTree.loc.pathToNode( op )
    } yield mtk
  }

  lazy val openedPath = openedLoc.map(
    nodesMap
      .mnsPath(_)
      .to( LazyList )
  )

  def openedNode = openedPath.flatMap(_.headOption)

  lazy val openedRcvrKey = openedPath.map( _.rcvrKey )

}
