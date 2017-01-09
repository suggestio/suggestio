package io.suggest.event.subscriber

import akka.actor.{ActorRef, ActorContext}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 18.02.14 14:15
 * Description: Бывает, что хочется просто подписать какой-то класс на обработку событий. Такое используется при
 * статической подписке на события или при нежелании генерить лишнии анонимные функции.
 */
trait SnClassSubscriber extends SnSubscriberT {
  override def toStringTail: String = getClass.getSimpleName
  override def getActor(implicit ctx: ActorContext): Option[ActorRef] = None
}
