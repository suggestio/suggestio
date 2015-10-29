package models.msys

import models.mgeo.MGsPtr
import models.{MGeoShape, MNode}
import play.api.data.Form

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 29.10.15 13:39
 * Description: Модель аргументов для шаблона editCircleTpl.
 */
trait ISysNodeGeoCircleEditTplArgs {
  def geo     : MGeoShape
  def cf      : Form[_]
  def mnode   : MNode
  def args0   : MGsPtr
}


case class MSysNodeGeoCircleEditTplArgs(
  override val geo     : MGeoShape,
  override val cf      : Form[_],
  override val mnode   : MNode,
  override val args0   : MGsPtr
)
  extends ISysNodeGeoCircleEditTplArgs
