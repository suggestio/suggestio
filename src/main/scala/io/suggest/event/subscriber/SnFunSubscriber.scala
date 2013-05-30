package io.suggest.event.subscriber

import io.suggest.event.SioNotifier
import akka.actor.{ActorRef, ActorContext}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 30.05.13 15:41
 * Description: Scala-класс для вешания анонимных функций на события SioNotifier.
 */
case class SnFunSubscriber(f: SioNotifier.Event => Unit) extends SnSubscriberT {

  /**
   * Передать событие подписчику.
   * @param event событие.
   * @param ctx контекст sio-notifier.
   */
  def publish(event: SioNotifier.Event)(implicit ctx: ActorContext) {
    val r = new Runnable {
      def run() { f(event) }
    }
    ctx.dispatcher.execute(r)
  }

  /**
   * Актор, относящийся к подписчику, если есть.
   * @return
   */
  def getActor(implicit ctx: ActorContext): Option[ActorRef] = ???

  /**
   * Хелпер для toString(). Выдаёт суффиксы.
   * @return
   */
  def toStringTail: String = "fun:" + f.toString()
}
