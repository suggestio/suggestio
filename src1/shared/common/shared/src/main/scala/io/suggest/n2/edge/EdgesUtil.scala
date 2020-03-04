package io.suggest.n2.edge

import io.suggest.primo.id.OptId

import scala.collection.MapView

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 20.09.17 10:10
  * Description: Кросс-платформенная утиль для работы с эджами.
  */
object EdgesUtil {

  def nextEdgeUid: EdgeUid_t = 0

  def nextEdgeUidFrom(edgeUids: IterableOnce[EdgeUid_t]): EdgeUid_t = {
    if (edgeUids.nonEmpty) {
      edgeUids.max + 1
    } else {
      nextEdgeUid
    }
  }

  def nextEdgeUidFromMap(edgeUidMap: Map[EdgeUid_t, _]): EdgeUid_t = {
    nextEdgeUidFrom( edgeUidMap.keys )
  }

  def purgeUnusedEdgesFromMap[E](usedEdgeIds: Set[EdgeUid_t], edgesMap: Map[EdgeUid_t, E]): MapView[EdgeUid_t, E] = {
    edgesMap
      .view
      .filterKeys { usedEdgeIds.contains }
  }

  // TODO Нужно задействовать CanBuildFrom[].
  def purgeUnusedEdgesFrom[E <: OptId[EdgeUid_t]](usedEdgeIds: Set[EdgeUid_t], edges: Iterable[E] ): Iterable[E] = {
    edges.filter(e => e.id.exists(usedEdgeIds.contains))
  }

}
