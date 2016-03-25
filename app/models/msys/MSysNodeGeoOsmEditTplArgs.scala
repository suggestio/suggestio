package models.msys

import io.suggest.model.n2.edge.MEdgeGeoShape
import models.mgeo.MGsPtr
import models.MNode
import play.api.data.Form

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 29.10.15 12:05
 * Description: Модель аргументов для вызова шаблона рендера шаблона
 * [[views.html.sys1.market.adn.geo.editAdnGeoOsmTpl]].
 */
trait ISysNodeGeoOsmEditTplArgs {
  def geo     : MEdgeGeoShape
  def gf      : Form[_]
  def mnode   : MNode
  def mGsPtr  : MGsPtr
}


case class MSysNodeGeoOsmEditTplArgs(
  override val geo     : MEdgeGeoShape,
  override val gf      : Form[_],
  override val mnode   : MNode,
  override val mGsPtr  : MGsPtr
)
  extends ISysNodeGeoOsmEditTplArgs
