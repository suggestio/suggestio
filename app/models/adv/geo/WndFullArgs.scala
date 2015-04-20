package models.adv.geo

import models.blk.IMadAndArgs
import models.{blk, MAdnNode, MAd}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 20.04.15 11:44
 * Description: Контейнер для базовых параметров реднера popup-окна.
 */
trait IWndFullArgs extends IMadAndArgs {

  /** Узел-создатель рекламной карточки. */
  def adProducer  : MAdnNode

  /** Ссылка для возврата. */
  def goBackTo    : Option[String]

}


/** Дефолтовая реализация [[IWndFullArgs]]. */
case class WndFullArgs(
  mad       : MAd,
  adProducer: MAdnNode,
  brArgs    : blk.RenderArgs,
  goBackTo  : Option[String] = None
)
  extends IWndFullArgs
