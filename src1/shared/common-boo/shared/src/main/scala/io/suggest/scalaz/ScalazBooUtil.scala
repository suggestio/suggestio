package io.suggest.scalaz

import boopickle.Default._
import scalaz.{IList, NonEmptyList, Tree}
import io.suggest.scalaz.ScalazUtil.Implicits._

object ScalazBooUtil {

  /** BooPickle support for z.NonEmptyList[X]. */
  implicit def scalazNelPickler[A: Pickler]: Pickler[NonEmptyList[A]] = {
    transformPickler[NonEmptyList[A], List[A]] { list =>
      NonEmptyList.nel( list.head,  IList.fromList(list.tail) )
    } { nel =>
      nel.list.toList
    }
  }


  /** BooPickle support for z.Tree[A]. */
  implicit def scalazTreePickler[A: Pickler]: Pickler[Tree[A]] = {
    def __toZTree(el: PickTree[A]): Tree[A] = {
      if (el.children.isEmpty) {
        Tree.Leaf( el.root )
      } else {
        val chs2 = el.children.map( __toZTree )
        Tree.Node( el.root, chs2.toEphemeralStream )
      }
    }

    def __fromZTree(t: Tree[A]): PickTree[A] = {
      val chs = t.subForest
        .map { __fromZTree }
        .toList
      PickTree.Node( t.rootLabel, chs )
    }

    transformPickler[Tree[A], PickTree[A]]( __toZTree )( __fromZTree )
  }

}


/** z.Tree is abstract class with anonymous impl.class with lazy fields.
  * To gain boopickle support, here is simple serializable clone of recursive z.Tree + it's implementation.
  */
sealed trait PickTree[A] {
  val root: A
  val children: List[PickTree[A]]
}
object PickTree {

  /** The only implementation of [[PickTree]]: just implement all fields as case class. */
  final case class Node[A](
                            override val root: A,
                            override val children: List[PickTree[A]]
                          )
    extends PickTree[A]

  implicit def pickTreeLeafP[A: Pickler]: Pickler[PickTree[A]] = {
    val p = compositePickler[PickTree[A]]
    p.addConcreteType[Node[A]]
  }

}


