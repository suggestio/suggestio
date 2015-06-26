package models.msc

import io.suggest.primo.IUnderlying
import models.MAdnNode

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 26.06.15 9:51
 * Description: Интерфейс продьюсера для различных моделей.
 */
trait IProducer {

  /** Экземпляр узла-продьюсера . */
  def producer  : MAdnNode

}

/** Враппер для IProducer. */
trait IProducerWrapper extends IProducer with IUnderlying {

  override def _underlying: IProducer

  override def producer = _underlying.producer
}
