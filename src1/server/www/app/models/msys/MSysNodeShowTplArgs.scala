package models.msys

import io.suggest.model.n2.edge.MEdge
import io.suggest.model.n2.node.MNode

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 26.10.15 19:06
 * Description: Модель аргументов для рендера шаблона [[views.html.sys1.market.adn.adnNodeShowTpl]].
 */

trait ISysNodeShowTplArgs {
  def mnode     : MNode
  def outEdges  : Seq[MNodeEdgeInfo]
  def inEdges   : Seq[MNodeEdgeInfo]
  // TODO legacy: это должно быть объединено с outEdges
  def personNames: Map[String, String]
}


/** Дефолтовая реализация модели [[ISysNodeShowTplArgs]]. */
case class MSysNodeShowTplArgs(
  override val mnode        : MNode,
  override val outEdges     : Seq[MNodeEdgeInfo],
  override val inEdges      : Seq[MNodeEdgeInfo],
  override val personNames  : Map[String, String]
)
  extends ISysNodeShowTplArgs


/** Модель контейнера системной информации по одном эджу. */
case class MNodeEdgeInfo(
  medge       : MEdge,
  mnodeEiths  : Seq[Either[String, MNode]],
  edgeId      : Option[Int]
)

