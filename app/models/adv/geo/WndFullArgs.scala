package models.adv.geo

import models.{blk, MAdnNode, MAd}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 20.04.15 11:44
 * Description: Контейнер для базовых параметров реднера popup-окна.
 */
trait IWndFullArgs {
  /** Рекламная карточка. */
  def mad         : MAd
  /** Узел-создатель рекламной карточки. */
  def adProducer  : MAdnNode
  /** Ссылка для возврата. */
  def goBackTo    : Option[String]
  /** Параметры рендера карточки. */
  def brArgs      : blk.RenderArgs
}

/** Дефолтовая реализация [[IWndFullArgs]]. */
case class WndFullArgs(
  mad       : MAd,
  adProducer: MAdnNode,
  brArgs    : blk.RenderArgs,
  goBackTo  : Option[String] = None
)
  extends IWndFullArgs
