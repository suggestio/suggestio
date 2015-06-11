package models.adv.geo

import models.blk.IBrArgs
import models.msc.IAdBodyTplArgs
import models.{blk, MAdnNode, MAd}

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
}


/** Дефолтовая реализация [[IWndFullArgs]]. */
case class WndFullArgs(
  producer  : MAdnNode,
  brArgs    : blk.RenderArgs,
  goBackTo  : Option[String] = None
)
  extends IWndFullArgs
