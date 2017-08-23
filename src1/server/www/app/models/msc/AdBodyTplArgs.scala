package models.msc

import io.suggest.model.n2.node.MNode
import models.mbase.{IProducer, IProducerWrapper}
import models.blk

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 11.06.15 16:25
 * Description: Контейнер аргументов, необходимых для рендера карточек выдачи.
 */

trait IAdBodyTplArgs extends IProducer {

  /** Аргументы рендера блока. */
  def brArgs    : blk.IRenderArgs

  /** Общее кол-во карточек в текущей выборке. */
  def adsCount  : Int

  /** Порядковый номер карточки в текущей выборке. */
  def index     : Int

  /** Является ли данная рекламная карточка размещенной сторонним продьюсером? */
  def is3rdParty: Boolean

}


/** Дефолтовая реализация [[IAdBodyTplArgs]]. */
case class AdBodyTplArgs(
  override val brArgs    : blk.IRenderArgs,
  override val producer  : MNode,
  override val adsCount  : Int,
  override val index     : Int,
  override val is3rdParty: Boolean
)
  extends IAdBodyTplArgs


trait IAdBodyTplArgsWrapper extends IAdBodyTplArgs with IProducerWrapper {
  override def _underlying: IAdBodyTplArgs

  override def brArgs   = _underlying.brArgs
  override def index    = _underlying.index
  override def adsCount = _underlying.adsCount
  override def is3rdParty = _underlying.is3rdParty
}
