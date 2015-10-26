package models.msys

import models.{MEdge, MNode}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 26.10.15 19:06
 * Description:
 */
case class MNodeEdgeInfo(
  medge     : MEdge,
  mnodeEith : Either[String, MNode]
)


trait ISysNodeShowTplArgs {
  def mnode     : MNode
  def outEdges  : Seq[MNodeEdgeInfo]
  def inEdges   : Seq[MNodeEdgeInfo]
  // TODO legacy: это должно быть объединено с outEdges
  def personNames: Map[String, String]
}


case class MSysNodeShowTplArgs(
  override val mnode        : MNode,
  override val outEdges     : Seq[MNodeEdgeInfo],
  override val inEdges      : Seq[MNodeEdgeInfo],
  override val personNames  : Map[String, String]
)
  extends ISysNodeShowTplArgs
