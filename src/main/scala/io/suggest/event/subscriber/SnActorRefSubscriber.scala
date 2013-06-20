package io.suggest.event.subscriber

import akka.actor.{ActorContext, ActorRef}
import io.suggest.event.SioNotifier

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 30.05.13 15:21
 * Description: Прямое подписывание другого актора на события.
 * @param actorRef реф актора.
 */
case class SnActorRefSubscriber(actorRef:ActorRef) extends SnSubscriberT {

  /**
   * Отправить сообщение этому актору.
   * @param event событие.
   * @param ctx контекст актора sio_notifier.
   */
  def publish(event: SioNotifier.Event)(implicit ctx:ActorContext) {
    actorRef forward event
  }

  def getActor(implicit ctx:ActorContext) = Some(actorRef)

  override def hashCode(): Int = actorRef.hashCode() + 10
  override def equals(obj: Any): Boolean = super.equals(obj) || {
    obj match {
      case SnActorRefSubscriber(_actor) => actorRef == _actor
      case _ => false
    }
  }
  def toStringTail: String = actorRef.toString()
}
