package models.mlk

import models.MAdnNode
import models.im.MImgT
import models.msc.ILogoImgOpt

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 30.09.15 12:08
 * Description: Модель передачи аргументов рендера в шаблон adnNodeShowTpl.
 */

trait INodeShowArgs extends ILogoImgOpt {
  def mnode       : MAdnNode
  def isMyNode    : Boolean
  def povAdnIdOpt : Option[String]
}

case class MNodeShowArgs(
  override val mnode       : MAdnNode,
  override val isMyNode    : Boolean,
  override val povAdnIdOpt : Option[String],
  override val logoImgOpt  : Option[MImgT]
)
  extends INodeShowArgs
