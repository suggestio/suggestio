package models.msys

import io.suggest.n2.edge.MEdgeGeoShape
import io.suggest.n2.node.MNode
import models.mgeo.MGsPtr
import play.api.data.Form

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 29.10.15 13:39
 * Description: Модель аргументов для шаблона [[views.html.sys1.market.adn.geo.editCircleTpl]].
 */
trait ISysNodeGeoCircleEditTplArgs {
  def geo     : MEdgeGeoShape
  def cf      : Form[_]
  def mnode   : MNode
  def args0   : MGsPtr
}


case class MSysNodeGeoCircleEditTplArgs(
  override val geo     : MEdgeGeoShape,
  override val cf      : Form[_],
  override val mnode   : MNode,
  override val args0   : MGsPtr
)
  extends ISysNodeGeoCircleEditTplArgs
