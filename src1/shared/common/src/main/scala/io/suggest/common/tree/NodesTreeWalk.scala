package io.suggest.common.tree

import io.suggest.adv.rcvr.RcvrKey
import io.suggest.primo.TypeT
import io.suggest.primo.id.IId

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 02.03.17 22:38
  * Description: Код гуляния по дереву моделей без каких-либо ограничений на тип модели.
  * Изначально рос внутри IRcvrPopupNode.
  */
trait NodesTreeApi extends TypeT {

  /** Вернуть все подузлы для указанного инстанса узла. */
  protected def _subNodesOf(node: T): TraversableOnce[T]

  /** Извлечь id узла. */
  protected def _nodeIdOf(node: T): String

}


trait NodesTreeWalk extends NodesTreeApi {

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
      findSubNode(rcvrKey, _subNodesOf(parent))
    }
  }


  /** Найти узел в списке узлов текущего уровня.
    *
    * @param rcvrKey Ключ узла.
    * @param nodes Список узлов верхнего уровня.
    * @return Опционально найденный узел.
    */
  def findSubNode(rcvrKey: RcvrKey, nodes: TraversableOnce[T]): Option[T] = {
    nodes
      .toIterator
      .filter { node => _nodeIdOf(node) == rcvrKey.head }
      .flatMap { subNode =>
        findSubNode(rcvrKey.tail, subNode)
      }
      .toStream
      .headOption
  }

}


/**
  * Частичная реализация [[NodesTreeWalk]] для случаев, когда узел является
  * реализацией [[io.suggest.primo.id.IId]][String].
  */
trait NodesTreeApiIId extends NodesTreeApi {

  override type T <: IId[String]

  override protected def _nodeIdOf(node: T): String = {
    node.id
  }

}
