package models.adv.geo

import models.blk.IBrArgs
import models.msc.IAdBodyTplArgs
import models.{blk, MNode}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 20.04.15 11:44
 * Description: Контейнер для базовых параметров реднера popup-окна.
 */
trait IWndFullArgs extends IAdBodyTplArgs with IBrArgs {

  /** Ссылка для возврата. */
  def goBackTo    : Option[String]

  override def index = 1
  override def adsCount = 1
  override def is3rdParty = false
}


/** Дефолтовая реализация [[IWndFullArgs]]. */
case class WndFullArgs(
  producer  : MNode,
  brArgs    : blk.RenderArgs,
  goBackTo  : Option[String] = None
)
  extends IWndFullArgs
