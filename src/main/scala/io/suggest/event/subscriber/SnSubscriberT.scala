package io.suggest.event.subscriber

import akka.actor.{ActorContext, ActorRef}
import io.suggest.event.SioNotifier

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 30.05.13 15:20
 * Description: Подписчик на шине. Он имеет метод publish(event, arf), скрывающий его внутреннюю структуру (актора, метод и тд)
 */

trait SnSubscriberT {

  /**
   * Передать событие подписчику.
   * @param event событие.
   * @param ctx контекст sio-notifier.
   */
  def publish(event:SioNotifier.Event)(implicit ctx:ActorContext)

  /**
   * Актор, относящийся к подписчику, если есть.
   * @return
   */
  def getActor(implicit ctx:ActorContext) : Option[ActorRef]

  /**
   * Хелпер для toString(). Выдаёт суффиксы.
   * @return
   */
  def toStringTail : String


  override def toString: String = "sn_subscriber:" + toStringTail
}
