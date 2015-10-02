package io.suggest.model.n2

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 23.09.15 11:27
 */
package object edge {

  type MPredicate = MPredicates.T

  /** Тип ключа карты эджей. */
  type NodeEdgesMapKey_t  = (MPredicate, String)
  /** Тип карты inline-эджей. */
  type NodeEdgesMap_t     = Map[NodeEdgesMapKey_t, MNodeEdge]

  implicit def EDGE_MAP_FORMAT = MNodeEdges.EMAP_FORMAT

}
