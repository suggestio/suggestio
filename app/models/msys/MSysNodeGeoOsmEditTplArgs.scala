package models.msys

import models.mgeo.MGsPtr
import models.{MNode, MGeoShape}
import play.api.data.Form

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 29.10.15 12:05
 * Description: Модель аргументов для вызова шаблона рендера шаблона
 * [[views.html.sys1.market.adn.geo.editAdnGeoOsmTpl]].
 */
trait ISysNodeGeoOsmEditTplArgs {
  def geo     : MGeoShape
  def gf      : Form[_]
  def mnode   : MNode
  def mGsPtr  : MGsPtr
}


case class MSysNodeGeoOsmEditTplArgs(
  override val geo     : MGeoShape,
  override val gf      : Form[_],
  override val mnode   : MNode,
  override val mGsPtr  : MGsPtr
)
  extends ISysNodeGeoOsmEditTplArgs
