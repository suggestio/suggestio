package io.suggest.scalaz

import io.suggest.common.empty.EmptyUtil
import io.suggest.msg.ErrorMsgs
import scalaz.{EphemeralStream, Tree, TreeLoc, Validation, ValidationNel}
import japgolly.univeq._
import scalaz.syntax.apply._
import play.api.libs.json._
import play.api.libs.functional.syntax._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 16.10.17 20:45
  * Description: Sio-утиль для модели дерева ScalaZ Tree.
  */
object ZTreeUtil {

  /** Поддержка play-json (обходчик вложенных implicit'ов) */
  private def _ZTREE_FORMAT[A: Format]: OFormat[Tree[A]] = {
    implicit var fmt: OFormat[Tree[A]] = null
    fmt = (
      (__ \ "r").format[A] and
      // По идее, implicitly[] дергает именно текущую implicit val fmt, а не ZTREE_FORMAT[].
      (__ \ "c").lazyFormatNullable( implicitly[Format[Seq[Tree[A]]]] )
        .inmap[Seq[Tree[A]]](
          EmptyUtil.opt2ImplEmpty1F( Nil ),
          { chs => if (chs.isEmpty) None else Some(chs) }
        )
        .inmap[EphemeralStream[Tree[A]]](
          EphemeralStream(_ : _*),
          EphemeralStream.toIterable(_).toStream
        )
    )( Tree.Node.apply(_, _), unlift(Tree.Node.unapply[A]) )
    fmt
  }

  /** Поддержка play-json. */
  implicit def ZTREE_FORMAT[A: Format]: OFormat[Tree[A]] = _ZTREE_FORMAT[A]

  implicit def zTreeUnivEq[A: UnivEq]: UnivEq[Tree[A]] = UnivEq.force
  implicit def zTreeLocUnivEq[A: UnivEq]: UnivEq[TreeLoc[A]] = UnivEq.force

  /** Высокоуровневая утиль для деревьев на базе Scalaz Tree.
    *
    * @param tree Rose tree.
    */
  implicit class ZTreeOps[A](private val tree: Tree[A] ) extends AnyVal {

    /** Выяснить путь в дереве до указанного узла.
      *
      * @param el Целевой тег.
      * @return Путь, если существует.
      */
    def nodeToPath(el: A)(implicit ue: UnivEq[A]): Option[NodePath_t] = {
      // Найти указанный элемент, а затем пройтись вверх
      tree.loc
        .findByLabel(el)
        .toNodePathOpt
    }


    // Старое API из IDocTag-дерева. Желательно портировать такой код на использование Tree/TreeLoc API напрямую.

    /** Итератор всех дочерних элементов со всех под-уровней. */
    def deepChildren: EphemeralStream[A] = {
      tree
        .flatten
        .tailOption
        .getOrElse( EphemeralStream.emptyEphemeralStream )
    }

    def deepSubtrees: EphemeralStream[Tree[A]] = {
      tree ##:: tree
        .subForest
        .flatMap(_.deepSubtrees)
    }

    def contains(jdt: A)(implicit ueq: UnivEq[A]): Boolean = {
      !tree
        .flatten
        .filter(jdt ==* _)
        .isEmpty
    }

    def deepMap(f: A => A): Tree[A] = {
      tree
        .loc
        .map(f)
        .toTree
    }


    /** Валидация узла и его дочерних узлов. */
    def validateNode[E](rootV   : A => ValidationNel[E, A])
                       (forestV : EphemeralStream[Tree[A]] => ValidationNel[E, EphemeralStream[Tree[A]]]): ValidationNel[E, Tree[A]] = {
      (
        rootV(tree.rootLabel) |@|
        forestV(tree.subForest)
      )( Tree.Node(_, _) )
    }

    /** Валидация узла без подчинённых узлов. */
    def validateLeaf[E](notLeafErr: => E)(rootV: A => ValidationNel[E, A]): ValidationNel[E, Tree[A]] = {
      validateNode(rootV) { forest =>
        Validation.liftNel(forest)(!_.isEmpty, notLeafErr)
      }
    }


    /** Проход сверху вниз по всем ветвям с отображением значений.
      * Нужен для сложного маппинга всего дерева с сохранением структуры,
      * например для впихивания id'шников узлов прямо в элементы дерева.
      * Порядок прохождения - сверху вглубь. На нижних уровнях аккамулятор терминируется.
      * @param accParent Аккамулятор, передаваемый сверху вглубь.
      * @param f Функция создания нового элемента и нового аккамулятора для возможных нижележащих уровней.
      * @return Ленивое дерево с прежней структурой.
      */
    def deepMapFold[Acc, T](accParent: Acc)(f: (Acc, Tree[A]) => (Acc, T) ): Tree[T] = {
      lazy val (accChild, el2) = f(accParent, tree)
      Tree.Node(
        root   = el2,
        forest = tree
          .subForest
          .map { chTree =>
            chTree.deepMapFold(accChild)(f)
          }
      )
    }


    /** Индексация дерева внутри всей уровней. */
    def zipWithIndex: Tree[(A, Int)] =
      zipWithIndex( startIndex = 0 )
    def zipWithIndex(startIndex: Int): Tree[(A, Int)] = {
      Tree.Node(
        root    = tree.rootLabel -> startIndex,
        forest  = tree
          .subForest
          .zipWithIndex
          .map { case (chTree, i) =>
            chTree.zipWithIndex(i)
          }
      )
    }

  }

  /** Цикл погружения в TreeLoc и получения оттуда.
    *
    * @param loc TreeLoc корня или текущего под-дерева.
    * @param restPath Оставшийся путь, который требуется пройти.
    * @tparam A Тип элемента дерева.
    * @return Опциональный результирующий TreeLoc.
    */
  private def _pathToNodeLoc[A](loc: TreeLoc[A], restPath: NodePath_t): Option[TreeLoc[A]] = {
    restPath
      .headOption
      .fold[Option[TreeLoc[A]]] {
        Some( loc )
      } { hd =>
        val chOpt = loc.getChild(hd + 1)
        if (chOpt.isEmpty)
          println( ErrorMsgs.JD_TREE_UNEXPECTED_CHILDREN, loc.tree.subForest.length )
        chOpt
          .flatMap { chLoc =>
            _pathToNodeLoc(chLoc, restPath.tail)
          }
      }
  }



  implicit class TreeLocOps[A](private val treeLoc: TreeLoc[A]) extends AnyVal {

    def toNodePath: NodePath_t = {
      val l0 = treeLoc.lefts.length
      // Пройтись вверх до самой макушки.
      val pathWithTop0 = treeLoc
        .parents
        .foldLeft[NodePath_t](l0 :: Nil) {
          case (acc, (pLefts, _, _)) =>
            pLefts.length :: acc
        }
      // Отбросить 0 с нулевого уровня.
      pathWithTop0.tail
    }


    /** Найти узел дерева по указанному пути.
      *
      * @param path Путь в дереве, полученный из nodePathTo().
      * @return Найденный тег, если найден.
      */
    def pathToNode(path: NodePath_t): Option[TreeLoc[A]] = {
      _pathToNodeLoc(treeLoc, path)
    }


    def findByLabel(a: A)(implicit ue: UnivEq[A]): Option[TreeLoc[A]] = {
      treeLoc
        .find { node =>
          node.getLabel ==* a
        }
    }

    def findUp(f: TreeLoc[A] => Boolean): Option[TreeLoc[A]] = {
      if ( f(treeLoc) ) {
        Some(treeLoc)
      } else {
        treeLoc
          .parent
          .flatMap( _.findUp(f) )
      }
    }

  }


  /** Доп.утиль для инстансов Option[TreeLoc]. */
  implicit class TreeLocOptOps[A](private val treeLocOpt: Option[TreeLoc[A]]) extends AnyVal {

    /** Сконвертить опциональный TreeLoc в опциональный элемент дерева. */
    def toLabelOpt: Option[A] = treeLocOpt.map(_.getLabel)

    def containsLabel(a: A)(implicit ue: UnivEq[A]): Boolean = {
      treeLocOpt.exists( _.getLabel ==* a )
    }

    def toNodePathOpt: Option[NodePath_t] = {
      treeLocOpt.map(_.toNodePath)
    }

  }


  implicit class TreeOptOps[A](private val treeOpt: Option[Tree[A]]) extends AnyVal {

    def containsLabel(a: A)(implicit ue: UnivEq[A]): Boolean = {
      treeOpt.exists(_.rootLabel ==* a)
    }

  }

}
