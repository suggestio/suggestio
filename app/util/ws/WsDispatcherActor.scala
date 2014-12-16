package util.ws

import akka.actor._
import akka.pattern.ask
import akka.util.Timeout
import play.libs.Akka
import util.{PlayMacroLogsImpl, SiowebSup}

import scala.collection.mutable
import scala.concurrent.Future
import scala.concurrent.duration._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 05.11.14 19:16
 * Description: Актор, поддерживающий карту wsId -> actorRef.
 */
object WsDispatcherActor {

  val ACTOR_NAME = "wsd"

  implicit val ASK_TIMEOUT = Timeout(5.seconds)

  val ACTOR_PATH = SiowebSup.actorPath / ACTOR_NAME

  def actorSelection = Akka.system.actorSelection(ACTOR_PATH)

  def startLink(arf: ActorRefFactory): ActorRef = {
    arf.actorOf(Props[WsDispatcherActor], name=ACTOR_NAME)
  }

  def getForWsId(wsId: String): Future[Option[ActorRef]] = {
    (actorSelection ? GetForId(wsId))
      .asInstanceOf[Future[Option[ActorRef]]]
  }

}


/** Актор, управляющий картой веб-сокетов.
  * ws-акторы запускаются и живут полностью асинхронно и довольно изолированно, поэтому нужна система связи с ними.
  * Карта с ключами wsId позволяет наладить связь между wsId на клиенте и актором тут. */
class WsDispatcherActor extends Actor with PlayMacroLogsImpl {

  import LOGGER._

  /** Карта ws-акторов. Ключ - это wsId (строка). */
  protected var wsMap: mutable.HashMap[String, ActorRef] = mutable.HashMap()

  override def preStart(): Unit = {
    super.preStart()
    trace(s"${getClass.getSimpleName} actor started.")
  }

  override def receive: Receive = {

    // Запрос адреса доставки
    case GetForId(wsId: String) =>
      val res = wsMap.get(wsId)
      trace(s"$self : wsId $wsId resolved TO $res")
      sender() ! res

    // Запустился актор, занимающийся обработкой ws-запросов.
    case WsActStarted(wsId: String) =>
      val r = sender()
      context.watch(r)
      wsMap.put(wsId, r)

    // Актор, занимавшийся обработкой ws-запросов остановился.
    case WsActStopped(wsId: String) =>
      context.unwatch(sender())
      wsMap.remove(wsId)

    // Внезапно остановился какой-то ws-актор, который вроде бы был исправен.
    case Terminated(actor) =>
      debug(s"Watched actor $actor unexpectedly stopped")
      wsMap = wsMap.filter {
        case (k, v)  =>  v != actor
      }
  }

}



sealed trait WsActMsg {
  def wsId: String
}

case class GetForId(wsId: String) extends WsActMsg
case class WsActStarted(wsId: String) extends WsActMsg
case class WsActStopped(wsId: String) extends WsActMsg

