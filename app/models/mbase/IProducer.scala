package models.mbase

import io.suggest.primo.IUnderlying
import models.MNode

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 26.06.15 9:51
 * Description: Интерфейс продьюсера для различных моделей.
 */
trait IProducer {

  /** Экземпляр узла-продьюсера . */
  def producer  : MNode

}

/** Враппер для IProducer. */
trait IProducerWrapper extends IProducer with IUnderlying {

  override def _underlying: IProducer

  override def producer = _underlying.producer
}
