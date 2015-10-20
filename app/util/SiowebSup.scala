package util

import com.google.inject.{Singleton, Inject}
import akka.actor._
import play.api.Application
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
@Singleton
class SiowebSup @Inject() (
  current       : Application
) { outer =>

  private def timeoutSec = 5.seconds

  private implicit def timeout = Timeout(timeoutSec)

  def actorName = "siowebSup"

  def actorPath = current.actorSystem / actorName

  def actorProps = Props( current.injector.instanceOf[SiowebSupActor] )

  /** Запуск супервизора в top-level. */
  def startLink(): ActorRef = {
    current.actorSystem.actorOf(actorProps, name = actorName)
  }

  type GetChildRefReply_t = Option[ActorRef]

  // Тип возвращаемого значения getChildRef.

  // Сообщение запроса дочернего процесса
  sealed case class GetChildRef(childName: String)

}


class SiowebSupActor @Inject() (
  companion: SiowebSup
) extends Actor with Logs {

  import companion.{GetChildRef, GetChildRefReply_t}

  /**
   * Нужно запустить все дочерние процессы.
   */
  override def preStart() {
    super.preStart()
    // Убедится, что статический actor-путь до этого супервизора стабилен и корректен.
    if (self.path != companion.actorPath) {
      throw new Exception(s"self.path==${self.path} but it must be equal to val ${companion.getClass.getSimpleName}.actorPath = ${companion.actorPath}")
    }
    // Запускаем все дочерние процессы.
    SiowebNotifier.startLink(context)
    WsDispatcherActor.startLink(context)
  }


  /**
   * Получение сообщения от кого-либо.
   * @return
   */
  def receive = {
    case GetChildRef(childName) =>
      val reply: GetChildRefReply_t = context.child(childName) // вручную проверяем тип из-за динамической типизации в akka.
      sender ! reply
  }


  /**
   * Стратеги супервайзинга этого актора.
   * @return
   */
  override def supervisorStrategy: SupervisorStrategy = {
    OneForOneStrategy(maxNrOfRetries = 10, withinTimeRange = 10.seconds) {
      case _: Exception => Restart
    }
  }

}

