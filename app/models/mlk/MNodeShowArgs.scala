package models.mlk

import models.MAdnNode
import models.im.MImgT
import models.msc.{IColors, ILogoImgOpt}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 30.09.15 12:08
 * Description: Модель передачи аргументов рендера в шаблон adnNodeShowTpl.
 */

trait INodeShowArgs
  extends ILogoImgOpt
  with IColors
{
  def mnode       : MAdnNode
  def isMyNode    : Boolean
  def povAdnIdOpt : Option[String]
}


case class MNodeShowArgs(
  override val mnode       : MAdnNode,
  override val isMyNode    : Boolean,
  override val povAdnIdOpt : Option[String],
  override val logoImgOpt  : Option[MImgT],
  override val bgColor     : String,
  override val fgColor     : String
)
  extends INodeShowArgs
