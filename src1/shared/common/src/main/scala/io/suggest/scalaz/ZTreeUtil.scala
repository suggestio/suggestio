package io.suggest.scalaz

import japgolly.univeq._

import scalaz.{Tree, TreeLoc}

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 16.10.17 20:45
  * Description: Sio-утиль для модели дерева ScalaZ Tree.
  */
object ZTreeUtil {


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
      tree
        .loc
        .find { node =>
          node.getLabel ==* el
        }
        .map { nodeLoc =>
          val l0 = nodeLoc.lefts.length
          // Пройтись вверх до самой макушки.
          val pathWithTop0 = nodeLoc.parents
            .iterator
            .foldLeft[NodePath_t](l0 :: Nil) {
              case (acc, (pLefts, _, _)) =>
                pLefts.length :: acc
            }
          // Отбросить 0 с нулевого уровня.
          pathWithTop0.tail
        }
    }


    /** Найти узел дерева по указанному пути.
      *
      * @param path Путь в дереве, полученный из nodePathTo().
      * @return Найденный тег, если найден.
      */
    def pathToNode(path: NodePath_t): Option[TreeLoc[A]] = {
      _pathToNodeLoc(tree.loc, path)
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


  /** Доп.утиль для инстансов Option[TreeLoc]. */
  implicit class TreeLocOptOps[A](private val treeLocOpt: Option[TreeLoc[A]]) extends AnyVal {

    /** Сконвертить опциональный TreeLoc в опциональный элемент дерева. */
    def toLabelOpt: Option[A] = treeLocOpt.map(_.getLabel)

  }

}
