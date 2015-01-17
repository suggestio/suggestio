package util

import play.api.Play.current
import play.api.libs.concurrent.Akka
import akka.actor._
import util.ws.WsDispatcherActor
import scala.concurrent.duration._
import akka.actor.SupervisorStrategy._
import concurrent.Await
import akka.pattern.ask
import akka.util.Timeout
import akka.actor.OneForOneStrategy
import util.event.SiowebNotifier
import util.urls_supply.SeedUrlsSupplier

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 26.04.13 10:06
 * Description: Супервизор процессов sioweb таких как менеджер процессов проверки qi, менеджер процессов валидации и т.д.
 * Когда запускается, также запускает дочерние вышеуказанные процессы.
 */

// Статический клиент к актору. Запускает всё дерево супервизора.
object SiowebSup {

  private val timeoutSec = 5.seconds
  private implicit val timeout = Timeout(timeoutSec)

  val actorName = "siowebSup"
  val actorPath = Akka.system / actorName


  /** Запуск супервизора в top-level. */
  def startLink: ActorRef = {
    Akka.system.actorOf(Props[SiowebSup], name=actorName)
  }

  type GetChildRefReply_t = Option[ActorRef]    // Тип возвращаемого значения getChildRef.

  /**
   * Хелпер для получения child-ref у супервизора. В целом, метод не нужный.
   * @param childName Имя дочернего актора.
   * @return
   */
  def getChildRef(childName: String) = {
    val sel = Akka.system.actorSelection(actorPath)
    Await.result(sel ? GetChildRef(childName), timeoutSec).asInstanceOf[GetChildRefReply_t]
  }

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
    NewsQueue4Play.startLinkSup(context)
    SiowebNotifier.startLink(context)
    SeedUrlsSupplier.startLink(context)
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

