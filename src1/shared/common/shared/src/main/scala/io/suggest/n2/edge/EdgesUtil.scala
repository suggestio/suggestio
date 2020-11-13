package io.suggest.n2.edge

import io.suggest.jd.MJdEdge

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

  // TODO Нужно задействовать CanBuildFrom[].
  def purgeUnusedEdgesFrom( usedEdgeIds: Set[EdgeUid_t], edges: Iterable[MJdEdge] ): Iterable[MJdEdge] = {
    edges.filter(e => e.edgeDoc.id.exists(usedEdgeIds.contains))
  }

}
