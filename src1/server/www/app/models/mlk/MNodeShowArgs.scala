package models.mlk

import models.MNode
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
  def mnode       : MNode
  def gallery     : Seq[MImgT]
}


case class MNodeShowArgs(
  override val mnode       : MNode,
  override val logoImgOpt  : Option[MImgT],
  override val bgColor     : String,
  override val fgColor     : String,
  override val gallery     : Seq[MImgT]
)
  extends INodeShowArgs
