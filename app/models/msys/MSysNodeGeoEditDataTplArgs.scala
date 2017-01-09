package models.msys

import models.MNode
import play.api.data.Form

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 29.10.15 14:39
 * Description: Модель аргументов для шаблона редактирования геоданных узла
 * [[views.html.sys1.market.adn.geo.editNodeGeodataTpl]].
 */
trait ISysNodeGeoEditDataTplArgs {
  def mnode       : MNode
  def gf          : Form[_]
  def nodesMap    : Map[String, MNode]
  def isProposed  : Boolean
}


case class MSysNodeGeoEditDataTplArgs(
  override val mnode       : MNode,
  override val gf          : Form[_],
  override val nodesMap    : Map[String, MNode],
  override val isProposed  : Boolean
)
  extends ISysNodeGeoEditDataTplArgs
