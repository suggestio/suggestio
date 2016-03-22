package io.suggest.model.n2

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 23.09.15 11:27
 */
package object edge {

  type MPredicate         = MPredicates.T

  /** Ключ эджа в карте помимо node id и predicate может содержать произвольные доп.данные в этом формате. */
  type EdgeXKey_t         = List[Any]

  /** Дефолтовое значение extra-ключа эджа. */
  def EdgeXKeyEmpty       = Nil

  /** Тип ключа карты эджей. */
  type NodeEdgesMapKey_t  = (MPredicate, Option[String], EdgeXKey_t)

  /** Тип карты inline-эджей. */
  type NodeEdgesMap_t     = Map[NodeEdgesMapKey_t, MEdge]

  implicit def EDGE_MAP_FORMAT = MNodeEdges.EMAP_FORMAT

}
