package io.suggest.scalaz

import io.suggest.common.empty.EmptyUtil

import scalaz.{Tree, TreeLoc}
import japgolly.univeq._
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
      (__ \ "c").lazyFormatNullable( implicitly[Format[Stream[Tree[A]]]] )
        .inmap[Stream[Tree[A]]](
          EmptyUtil.opt2ImplEmpty1F( Stream.empty ),
          { chs => if (chs.isEmpty) None else Some(chs) }
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
  implicit class ZTreeJdOps[A]( private val tree: Tree[A] ) extends AnyVal {

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


    /** Найти узел дерева по указанному пути.
      *
      * @param path Путь в дереве, полученный из nodePathTo().
      * @return Найденный тег, если найден.
      */
    def pathToNode(path: NodePath_t): Option[TreeLoc[A]] = {
      _pathToNodeLoc(tree.loc, path)
    }


    // Старое API из IDocTag-дерева. Желательно портировать такой код на использование Tree/TreeLoc API напрямую.

    /** Итератор всех дочерних элементов со всех под-уровней. */
    def deepChildren: Stream[A] = {
      tree.flatten.tail
    }

    def contains(jdt: A): Boolean = {
      tree.flatten.contains( jdt )
    }

    def deepMap(f: A => A)(implicit ue: UnivEq[A]): Tree[A] = {
      tree
        .loc
        .map(f)
        .toTree
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
        loc
          .getChild(hd + 1)   // +1, потому что Tree считает с 1, а не с 0.
          .flatMap { chLoc => _pathToNodeLoc(chLoc, restPath.tail) }
      }
  }



  implicit class TreeLocOps[A](private val treeLoc: TreeLoc[A]) extends AnyVal {

    def toNodePath: NodePath_t = {
      val l0 = treeLoc.lefts.length
      // Пройтись вверх до самой макушки.
      val pathWithTop0 = treeLoc.parents
        .iterator
        .foldLeft[NodePath_t](l0 :: Nil) {
          case (acc, (pLefts, _, _)) =>
            pLefts.length :: acc
        }
      // Отбросить 0 с нулевого уровня.
      pathWithTop0.tail
    }


    def findByLabel(a: A)(implicit ue: UnivEq[A]): Option[TreeLoc[A]] = {
      treeLoc
        .find { node =>
          node.getLabel ==* a
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
