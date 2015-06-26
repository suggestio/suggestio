package models.mdr

import models.msc.IAdBodyTplArgs
import models.{MAdnNode, blk}
import play.api.data.Form

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 11.06.15 16:11
 * Description: Аргументы для рендера шаблона sys/freeAdvMdrTpl.
 * Для возможности рендера рекламной карточки, модель расширяет интерфейс AdBodyTplArgs.
 */
trait IFreeAdvMdrRArgs extends IAdBodyTplArgs {

  def banFormM: Form[String]

  override def index    = 0
  override def adsCount = 0
  override def is3rdParty = false
}


/** Дефолтовая реализация [[IFreeAdvMdrRArgs]]. */
case class FreeAdvMdrRArgs(
  override val brArgs   : blk.IRenderArgs,
  override val producer : MAdnNode,
  override val banFormM : Form[String]
)
  extends IFreeAdvMdrRArgs

