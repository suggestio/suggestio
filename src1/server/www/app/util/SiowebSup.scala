package util

import com.google.inject.{Inject, Singleton}
import akka.actor._
import play.api.Application
import util.ws.WsDispatcherActors

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

object SiowebSupActor {
  // TODO Вынести эту статику в модели. А сам актор в какую-нить поддиректорию затолкать.

  // Тип возвращаемого значения getChildRef.
  type GetChildRefReply_t = Option[ActorRef]

  // Сообщение запроса дочернего процесса
  sealed case class GetChildRef(childName: String)

}


// Статический клиент к актору. Запускает всё дерево супервизора.
@Singleton
class SiowebSup @Inject() (
  current       : Application
) { outer =>

  // constructor
  startLink()

  private def timeoutSec = 5.seconds

  private implicit def timeout = Timeout(timeoutSec)

  def actorName = "siowebSup"

  def actorPath = current.actorSystem / actorName

  def actorProps = Props( current.injector.instanceOf[SiowebSupActor] )

  /** Запуск супервизора в top-level. */
  def startLink(): ActorRef = {
    current.actorSystem.actorOf(actorProps, name = actorName)
  }

}


class SiowebSupActor @Inject() (
  wsDispatcherActors  : WsDispatcherActors,
  siowebNotifier      : SiowebNotifier
) extends Actor with Logs {

  import SiowebSupActor.{GetChildRef, GetChildRefReply_t}

  /**
   * Нужно запустить все дочерние процессы.
   */
  override def preStart() {
    super.preStart()
    // Запускаем все дочерние процессы.
    siowebNotifier.startLink(context)
    wsDispatcherActors.startLink(context)
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

