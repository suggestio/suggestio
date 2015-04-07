package util

import play.api.Play.current
import play.api.libs.concurrent.Akka
import akka.actor._
import util.ws.WsDispatcherActor
import scala.concurrent.duration._
import akka.actor.SupervisorStrategy._
import akka.util.Timeout
import akka.actor.OneForOneStrategy
import util.event.SiowebNotifier

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 26.04.13 10:06
 * Description: Супервизор процессов sioweb таких как менеджер процессов проверки qi, менеджер процессов валидации и т.д.
 * Когда запускается, также запускает дочерние вышеуказанные процессы.
 */

// Статический клиент к актору. Запускает всё дерево супервизора.
object SiowebSup {

  private def timeoutSec = 5.seconds
  private implicit def timeout = Timeout(timeoutSec)

  def actorName = "siowebSup"
  def actorPath = Akka.system / actorName


  /** Запуск супервизора в top-level. */
  def startLink(system: ActorSystem): ActorRef = {
    system.actorOf(Props[SiowebSup], name=actorName)
  }

  type GetChildRefReply_t = Option[ActorRef]    // Тип возвращаемого значения getChildRef.

  // Сообщение запроса дочернего процесса
  sealed case class GetChildRef(childName: String)
}


import SiowebSup._

class SiowebSup extends Actor with Logs {

  /**
   * Нужно запустить все дочерние процессы.
   */
  override def preStart() {
    super.preStart()
    // Убедится, что статический actor-путь до этого супервизора стабилен и корректен.
    if (self.path != SiowebSup.actorPath) {
      throw new Exception("self.path==%s but it must be equal to val %s.actorPath = %s" format (self.path, SiowebSup.getClass.getSimpleName, actorPath))
    }
    // Запускаем все дочерние процессы.
    SiowebNotifier.startLink(context)
    billing.StatBillingQueueActor.startLink(context)
    WsDispatcherActor.startLink(context)
  }


  /**
   * Получение сообщения от кого-либо.
   * @return
   */
  def receive = {
    case GetChildRef(childName) =>
      val reply: GetChildRefReply_t = context.child(childName)  // вручную проверяем тип из-за динамической типизации в akka.
      sender ! reply
  }


  /**
   * Стратеги супервайзинга этого актора.
   * @return
   */
  override def supervisorStrategy: SupervisorStrategy = {
    OneForOneStrategy(maxNrOfRetries = 10, withinTimeRange = 10 seconds) {
      case _:Exception => Restart
    }
  }

}

