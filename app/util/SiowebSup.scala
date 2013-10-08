package util

import play.api.Play.current
import play.api.libs.concurrent.Akka
import akka.actor._
import scala.concurrent.duration._
import akka.actor.SupervisorStrategy._
import concurrent.Await
import akka.pattern.ask
import akka.util.Timeout
import akka.actor.OneForOneStrategy
import io.suggest.SioutilSup
import DomainRequester.{ACTOR_NAME => DOMAIN_REQUESTER_NAME}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 26.04.13 10:06
 * Description: Супервизор процессов sioweb таких как менеджер процессов проверки qi, менеджер процессов валидации и т.д.
 * Когда запускается, также запускает дочерние вышеуказанные процессы.
 */

class SiowebSup extends Actor with Logs {

  /**
   * Нужно запустить все дочерние процессы.
   */
  override def preStart() {
    super.preStart()
    // Запускаем все дочерние процессы.
    DomainRequester.startLink(context)
    SioutilSup.start_link(context)
    NewsQueue4Play.startLinkSup(context)
    //SiobixClient.startLink(context)   // TODO включить, когда будет готов, и удалить это TODO
  }


  /**
   * Получение сообщения от кого-либо.
   * @return
   */
  def receive = {
    case GetChildRef(childName) =>
      sender ! context.child(childName)
  }


  /**
   * Стратеги супервайзинга этого актора.
   * @return
   */
  override def supervisorStrategy: SupervisorStrategy =
    OneForOneStrategy(maxNrOfRetries = 10, withinTimeRange = 10 seconds) {
      case _:Exception => Restart
    }

}


// Статический клиент к актору. В частности, запускает супервизора и хранит его ref.
object SiowebSup {

  private val timeoutSec = 5.seconds
  private implicit val timeout = Timeout(timeoutSec)

  private val supRef = {
    val props = Props[SiowebSup]
    Akka.system.actorOf(props, name="sioweb_sup")
  }

  /**
   * Хелпер для получения child-ref у супервизора.
   * @param childName
   * @return
   */
  protected def getChildRef(childName:String) = Await.result(supRef ? GetChildRef(childName), timeoutSec).asInstanceOf[Option[ActorRef]]

  /**
   * Выдать ref менеджера менеджера запросов в доменам-сайтам.
   * @return ActorRef
   */
  def getDomainRequesterRef = getChildRef(DOMAIN_REQUESTER_NAME).get


  def ensureStarted = {
    !supRef.isTerminated
  }

}


// Сообщение запроса дочернего процесса
protected final case class GetChildRef(childName:String)
