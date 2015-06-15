package models.msc

import models.{blk, MAdnNode}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 11.06.15 16:25
 * Description: Контейнер аргументов, необходимых для рендера карточек выдачи.
 */

trait IAdBodyTplArgs {

  /** Аргументы рендера блока. */
  def brArgs    : blk.IRenderArgs

  /** Экземпляр продьюсера карточки. */
  def producer  : MAdnNode

  /** Общее кол-во карточек в текущей выборке. */
  def adsCount  : Int

  /** Порядковый номер карточки в текущей выборке. */
  def index     : Int

}


/** Дефолтовая реализация [[IAdBodyTplArgs]]. */
case class AdBodyTplArgs(
  override val brArgs    : blk.IRenderArgs,
  override val producer  : MAdnNode,
  override val adsCount  : Int,
  override val index     : Int
)
  extends IAdBodyTplArgs


trait IAdBodyTplArgsWrapper extends IAdBodyTplArgs {
  def _underlying: IAdBodyTplArgs

  override def brArgs   = _underlying.brArgs
  override def producer = _underlying.producer
  override def index    = _underlying.index
  override def adsCount = _underlying.adsCount
}
