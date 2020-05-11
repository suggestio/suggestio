package io.suggest.common.tree

import io.suggest.adv.rcvr.RcvrKey
import japgolly.univeq._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 03.03.17 14:54
  * Description: Система обновления узлов в immutable-дереве узлов.
  */

trait NodeTreeUpdate extends NodesTreeApi {

  def withNodeChildren(node: T, children2: IterableOnce[T]): T

  type FlatMapF_t = T => IterableOnce[T]


  def flatMapNode(rcvrKey: RcvrKey, node: T)(updateF: FlatMapF_t): IterableOnce[T] = {
    // Случай пустого rcvrKey НЕ игнорируем, т.к. это скорее защита от самого себя.
    val nodeId = _nodeIdOf(node)
    if ( rcvrKey.headOption.contains(nodeId) )
      flatMapSubNode(rcvrKey.tail, node)(updateF)
    else
      node :: Nil
  }

  /** Обновление какого-то пол-узла в дереве с корнем parent с помощью функции.
    * Считаем, что идёт погружение в дерево, и пустой rcvrKey означает, что найден подходящий узел.
    *
    * @param rcvrKey Путь по узла.
    * @param parent Родительский элемент.
    * @param updateF Функция обновления целевого узла в коллекцию новых узлов.
    *                [] - элемент будет удалён.
    *                [...] элемет будет заменён на указанные элементы..
    * @return Обновлённое дерево узлов.
    */
  def flatMapSubNode(rcvrKey: RcvrKey, parent: T)(updateF: FlatMapF_t): IterableOnce[T] = {
    if (rcvrKey.isEmpty) {
      updateF( parent )
    } else {
      val res = withNodeChildren(
        node      = parent,
        children2 = flatMapSubNode(rcvrKey, _subNodesOf(parent))(updateF)
      )
      res :: Nil
    }
  }

  def flatMapSubNode(rcvrKey: RcvrKey, nodes: IterableOnce[T])(updateF: FlatMapF_t): Iterator[T] = {
    nodes
      .iterator
      .flatMap { node =>
        if ( _nodeIdOf(node) ==* rcvrKey.head ) {
          // Этот элемент подходит под ключ. Значит, надо погружаться в него.
          flatMapSubNode(rcvrKey.tail, node)(updateF)
        } else {
          // Этот элемент мимо кассы, не трогаем его.
          node :: Nil
        }
      }
  }


  def updateChildren(rcvrKey: RcvrKey, nodes: IterableOnce[T])(updateF: IterableOnce[T] => IterableOnce[T]): IterableOnce[T] = {
    if (rcvrKey.isEmpty) {
      updateF(nodes)
    } else {
      flatMapSubNode(rcvrKey, nodes) { node0 =>
        withNodeChildren(
          node0,
          updateF(_subNodesOf(node0))
        ) :: Nil
      }
    }
  }

  /** Функция удаления элемента.
    * Её можно передавать в качестве значения updateF для удаления элемента по указанному пути.
    */
  def deleteF: FlatMapF_t = { _ => Nil }

}
