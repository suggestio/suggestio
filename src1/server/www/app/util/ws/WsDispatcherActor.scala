package util.ws

import akka.actor._
import akka.pattern.ask
import akka.util.Timeout
import javax.inject.{Inject, Singleton}

import io.suggest.util.logs.MacroLogsImpl
import models.mproj.ICommonDi
import util.SiowebSup

import scala.collection.mutable
import scala.concurrent.Future
import scala.concurrent.duration._
import scala.util.{Failure, Success}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 05.11.14 19:16
 * Description: Актор, поддерживающий карту wsId -> actorRef.
 */
@Singleton
class WsDispatcherActors @Inject() (mCommonDi: ICommonDi) extends MacroLogsImpl {

  import mCommonDi._

  def ACTOR_NAME = "wsd"

  implicit def ASK_TIMEOUT = Timeout(5.seconds)

  private lazy val siowebSup = current.injector.instanceOf[SiowebSup]
  def ACTOR_PATH = siowebSup.actorPath / ACTOR_NAME

  def actorSelection = actorSystem.actorSelection(ACTOR_PATH)

  def startLink(arf: ActorRefFactory): ActorRef = {
    arf.actorOf(Props[WsDispatcherActor](), name=ACTOR_NAME)
  }

  def getForWsId(wsId: String): Future[Option[ActorRef]] = {
    (actorSelection ? GetForId(wsId))
      .asInstanceOf[Future[Option[ActorRef]]]
  }



  /** Сколько асинхронных попыток предпринимать. */
  private def NOTIFY_WS_WAIT_RETRIES_MAX = 10

  /** Пауза между повторными попытками отправить уведомление. */
  private def NOTIFY_WS_RETRY_PAUSE = 2.seconds

  /** Послать сообщение ws-актору с указанным wsId. Если WS-актор ещё не появился, то нужно подождать его
    * некоторое время. Если WS-актор так и не появился, то выразить соболезнования в логи. */
  def notifyWs(wsId: String, msg: Any, counter: Int = 0): Unit = {
    getForWsId(wsId)
      .onComplete {
        case Success(Some(wsActorRef)) =>
          wsActorRef ! msg
        case other =>
          lazy val logPrefix = s"notifyWs($wsId, try=$counter/$NOTIFY_WS_WAIT_RETRIES_MAX):"
          if (counter < NOTIFY_WS_WAIT_RETRIES_MAX) {
            actorSystem.scheduler
              .scheduleOnce(NOTIFY_WS_RETRY_PAUSE) {
                notifyWs(wsId, msg, counter + 1)
              }
            def warnMsg = s"$logPrefix Failed to ask ws-actor-dispatcher about WS actor"
            other match {
              case Failure(ex) => LOGGER.warn(warnMsg, ex)
              case _           => LOGGER.debug(warnMsg)
            }
          } else {
            def debugMsg = s"$logPrefix WS message was not sent and dropped, because actor not found: $msg , Last error was:"
            other match {
              case Failure(ex) => LOGGER.debug(debugMsg, ex)
              case _           => LOGGER.debug(debugMsg)
            }
          }
      }
  }

}

/** Интерфейс DI-поля с инстансом [[WsDispatcherActors]]. */
trait IWsDispatcherActorsDi {
  /** Инстанс [[WsDispatcherActors]]. */
  def wsDispatcherActors: WsDispatcherActors
}


/** Актор, управляющий картой веб-сокетов.
  * ws-акторы запускаются и живут полностью асинхронно и довольно изолированно, поэтому нужна система связи с ними.
  * Карта с ключами wsId позволяет наладить связь между wsId на клиенте и актором тут. */
class WsDispatcherActor extends Actor with MacroLogsImpl {

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
      //trace(s"$self : wsId $wsId resolved TO $res")
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
        case (_, v)  =>  v != actor
      }
  }

}



sealed trait WsActMsg {
  def wsId: String
}

case class GetForId(wsId: String) extends WsActMsg
case class WsActStarted(wsId: String) extends WsActMsg
case class WsActStopped(wsId: String) extends WsActMsg

