package io.suggest.lk.nodes

import io.suggest.adv.rcvr.RcvrKey

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 02.03.17 22:38
  * Description: Код гуляния по дереву моделей.
  * Изначально рос внутри IRcvrPopupNode.
  */
trait NodesTreeWalker[T] {

  protected def _subNodesOf(node: T): TraversableOnce[T]

  protected def _nodeIdOf(node: T): String

  /** Рекурсивный поиск под-узла по указанному пути id.
    * @param rcvrKey Путь id'шников узла.
    * @param parent Начальный узел.
    * @return Опционально: найденный под-узел.
    *         Если путь пустой, то будет возвращён текущий узел.
    */
  def findSubNode(rcvrKey: RcvrKey, parent: T): Option[T] = {
    if (rcvrKey.isEmpty) {
      Some(parent)
    } else {
      val childRcvrKey = rcvrKey.tail
      _subNodesOf(parent)
      //parent.subGroups
      //  .iterator
      //  .flatMap(_.nodes)
        .toIterator
        .filter { node => _nodeIdOf(node) == rcvrKey.head }
        .flatMap { subNode =>
          findSubNode(childRcvrKey, subNode)
        }
        .toStream
        .headOption
    }
  }

  /**
    * Строгий поиск узла по указанному node-id пути.
    * В поиске участвует текущий узел и его под-узлы.
    * @param rcvrKey Ключ узла.
    * @param node Начальный узел.
    * @return Опционально: найденный узел.
    */
  def findNode(rcvrKey: RcvrKey, node: T): Option[T] = {
    // Случай пустого rcvrKey НЕ игнорируем, т.к. это скорее защита от самого себя.
    val nodeId = _nodeIdOf(node)
    if ( rcvrKey.headOption.contains(nodeId) )
      findSubNode(rcvrKey.tail, node)
    else
      None
  }

}
