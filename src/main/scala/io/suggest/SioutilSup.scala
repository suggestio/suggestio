package io.suggest

import akka.actor.{ActorRef, ActorRefFactory, Actor, Props}
import io.suggest.event.{SioNotifier, SioNotifierWatcher}
import akka.pattern.ask
import scala.concurrent.duration._
import akka.util.Timeout
import scala.concurrent.Future

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 29.04.13 18:25
 * Description: Супервизор для акторов, лежащих в sioutil.
 */

class SioutilSup extends Actor {

  /**
   * Запуск супервизора - запустить дочерние процессы
   */
  override def preStart() {
    super.preStart()

    context.actorOf(Props[SioNotifierWatcher], name=SioNotifier.SN_WATCHER_NAME)
  }

  /**
   * Обработка сообщений.
   * @return
   */
  def receive = {
    // Получено имя child-актора. Нужно вернуть ActorRef, если такой имеется.
    case name: String =>
      val childRefOpt = context.child(name)
      sender ! childRefOpt

    // Получено сообщение об остановке супервизора
    case Stop() =>
      context.stop(self)
  }

}


object SioutilSup {

  // Костыль, ибо у akka нет постоянных идентификаторов акторов, по которым с ними можно связаться.
  protected var sup_ref : ActorRef = null

  val SUP_NAME = "sioutil_sup"
  private implicit val timeout = Timeout(2.seconds)

  /**
   * Запуск супервизора в контексте другой подсистемы actor-ов
   * @param arf ActorSystem | ActorContext | etc
   * @return ActorRef
   */
  def start_link(arf:ActorRefFactory) : ActorRef = {
    sup_ref = arf.actorOf(Props[SioutilSup], name=SUP_NAME)
    sup_ref
  }

  /**
   * Выдать дочернего актора текущего супервизора.
   * @param name имя дочернего процесса
   * @return Option[ActorRef]
   */
  def getChild(name:String) = (sup_ref ? name).asInstanceOf[Future[Option[ActorRef]]]

  /**
   * Остановка супервизора.
   */
  def stop {
    sup_ref ! Stop()
  }

}

final case class Stop()
