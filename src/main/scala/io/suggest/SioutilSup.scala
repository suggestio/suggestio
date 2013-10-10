package io.suggest

import akka.actor._
import akka.pattern.ask
import scala.concurrent.duration._
import akka.util.Timeout
import scala.concurrent.Future
import io.suggest.event.SioNotifier

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 29.04.13 18:25
 * Description: Супервизор для акторов, лежащих в sioutil.
 */

// TODO Вероятно, это надо удалить.

class SioutilSup extends Actor {

  /**
   * Запуск супервизора - запустить дочерние процессы
   */
  override def preStart() {
    super.preStart()

    SioNotifier.startLink(context)
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

    case ResolveActorPath(path) =>
      sender ! context.system.actorFor(path)

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
   * Отрезолвить путь актора. Если актора не существует, то придет ref на dead letters.
   * @param actorPath путь актора
   * @return
   */
  def resolveActorPath(actorPath:ActorPath) = (sup_ref ? ResolveActorPath(actorPath)).asInstanceOf[Future[ActorRef]]

  /**
   * Остановка супервизора.
   */
  def stop {
    sup_ref ! Stop()
  }

}

protected final case class ResolveActorPath(path:ActorPath)
protected final case class Stop()
