package io.suggest.event.subscriber

import akka.actor.{ActorContext, ActorPath}
import io.suggest.event.SioNotifier.Event

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 30.05.13 15:23
 * Description: Актор задан через путь. Связаваться с супервизором, чтоб он отрезовлвил путь.
 * @param actorPath путь до актора
 */
// TODO использовать ActorSystem вместо контекста актора.

case class SnActorPathSubscriber(actorPath:ActorPath) extends SnSubscriberT {

  /**
   * Найти актора и отправить ему послание. Если актора уже нет, то сообщение будет отправлено к актору dead letters.
   * @param event событие.
   * @param ctx для возможности резолва ActorPath в системе Akka.
   */
  def publish(event: Event)(implicit ctx:ActorContext) {
    ctx.actorSelection(actorPath) ! event
  }

  def getActor(implicit ctx:ActorContext) = None

  override def hashCode(): Int = actorPath.hashCode() + 31
  override def equals(obj: Any): Boolean = super.equals(obj) || {
    obj match {
      case SnActorPathSubscriber(_actorPath) => actorPath == _actorPath
      case _ => false
    }
  }

  def toStringTail: String = actorPath.toString
}