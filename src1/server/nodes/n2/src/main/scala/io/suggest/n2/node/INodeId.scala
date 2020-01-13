package io.suggest.n2.node

/** Все MNode-события имеют nodeId. И не только события.
  * Тут интерфейс, описывающий это поле. */
trait INodeId {

  /** id узла [[MNode]]. */
  def nodeId: String

}
