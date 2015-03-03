package util.adv

import _root_.util.PlayMacroLogsImpl
import _root_.util.async.FsmActor
import _root_.util.ws.SubscribeToWsDispatcher
import akka.actor.{Actor, ActorRef, Props}
import models.adv._
import models.adv.js.ctx.MJsCtx
import models.adv.js._
import play.api.libs.json._
import play.api.libs.concurrent.Execution.Implicits.defaultContext

import scala.annotation.tailrec
import scala.collection.immutable.Queue
import scala.util.{Random, Failure, Success}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 26.12.14 16:24
 * Description: Утиль и код актора, который занимается общением с js api размещения рекламных карточек на клиенте.
 */
object ExtAdvWsActor {

  /** Сборка конфигурации актора. */
  def props(out: ActorRef, eactx: IExtWsActorArgs): Props = {
    Props(ExtAdvWsActor(out, eactx))
  }

}


/** ws-актор, готовый к использованию websocket api. */
case class ExtAdvWsActor(out: ActorRef, eactx: IExtWsActorArgs)
  extends FsmActor
  with SubscribeToWsDispatcher
  with PlayMacroLogsImpl
{ actor =>

  import LOGGER._

  /** Флаг, обозначающий, что на клиент отправлена ask-команда, и он занимается обработкой оной.
    * Если флаг true, то значит js-система занимается какими-то действиями, скорее всего юзеру какое-то окно
    * отображено. */
  protected var _jsAskLock: Boolean = false

  /** Очередь команд для клиента. */
  protected var _queue = Queue[IWsCmd]()

  val rnd = new Random()

  /** Сериализация и отправка одной js-команды в веб-сокет. */
  def sendJsCommand(jsCmd: IWsCmd): Unit = {
    out ! Json.toJson(jsCmd)
  }

  /** Получена js-команда от какого-то актора. Нужно в зависимости от ситуации отправить её в очередь
    * или же отправить немедлено, выставив флаг блокировки. */
  def enqueueCommand(ask: IWsCmd): Unit = {
    if (_jsAskLock) {
      // Клиент отрабатывает другой ask. Значит отправить в очередь.
      trace("Asking still locked. 1 command queued.")
      _queue = _queue.enqueue(ask)
    } else {
      trace("Client ready for asking. Sending command and locking...")
      // Клиент свободен. Считаем, что очередь пуста. Сразу отправляем, выставляя флаг.
      sendJsCommand(ask)
      _jsAskLock = true
    }
  }

  /** Необходимо пропедалировать очередь команд вперёд в связи с получением какого-то ответа от js. */
  def dequeueAnswerReceived(): Unit = {
    if (_queue.isEmpty) {
      // Очередь пуста. Снимает флаг блокировки.
      trace("Queue empty. Unlocking...")
      _jsAskLock = false
    } else {
      // В очереди ещё есть запросы к js -- отправляем следующий запрос.
      trace("Dequeuing & sending next command...")
      val (ask, newQueue) = _queue.dequeue
      sendJsCommand(ask)
      _queue = newQueue
      _jsAskLock = true
    }
  }

  /** Подписка на WsDispatcher требует указания wsId. */
  override def wsId: String = eactx.qs.wsId

  override def allStatesReceiver: Receive = PartialFunction.empty
  override def receive: Actor.Receive = allStatesReceiver

  /** Текущее состояние FSM хранится здесь. */
  override protected var _state: FsmState = new DummyState

  override def preStart(): Unit = {
    super.preStart()
    become(new WaitForTargetsState)
  }

  /** Придумываем рандомный идентификатор для нового актора. По этому id будут роутится js-ответы. */
  @tailrec final def guessChildName(): String = {
    val actorName = Math.abs(rnd.nextLong()).toString
    if ( context.child(actorName).isDefined )
      guessChildName()
    else
      actorName
  }

  /** Состояние ожидания асинхронных данных по целям для размещения, запущенных в предыдущем состоянии. */
  class WaitForTargetsState extends FsmState {
    override def afterBecome(): Unit = {
      super.afterBecome()
      // Повесить callback'и на фьючерс с таргетами.
      eactx.targetsFut onComplete {
        case Success(targets) => self ! TargetsReady(targets.toList)
        case Failure(ex)      => self ! TargetsFailed(ex)
      }
    }

    override def receiverPart: Receive = {
      case TargetsReady(targets) =>
        val tisCount = eactx.qs.targets.size
        val tgtsCount = targets.size
        if (tisCount != tgtsCount) {
          warn(s"Unexpected number of targets read from model: $tgtsCount, but $tisCount expected.\n  requested = ${eactx.qs.targets}  found = $targets")
        }
        if (targets.isEmpty) {
          throw new IllegalStateException("No targets found in storage, but it should.")
        } else {
          trace(s"$name waiting finished. Found ${targets.size} targets.")
          // Сгруппировать таргеты по сервисам, запустить service-акторов, которые занимаются инициализацией клиентов сервисов.
          targets.groupBy(_.target.service).foreach {
            case (_service, _srvTgs) =>
              trace(s"Starting service ${_service} actor with ${_srvTgs.size} targets...")
              val actorArgs = new IExtAdvServiceActorArgs with IExtAdvArgsWrapperT {
                override def service            = _service
                override def targets            = _srvTgs
                override def _eaArgsUnderlying  = eactx
                override def wsMediatorRef      = self
              }
              context.actorOf(ExtServiceActor.props(actorArgs), name = guessChildName())
            }
          become(new SuperviseServiceActorsState)
        }

      case TargetsFailed(ex) =>
        error(s"$name: Failed to aquire targers", ex)
        become(new DummyState)
    }

    case class TargetsReady(targets: ActorTargets_t)
    case class TargetsFailed(ex: Throwable)
  }


  /** Состояние обработки двунаправленного обмена сообщениями. */
  class SuperviseServiceActorsState extends FsmState {
    override def receiverPart: Receive = {
      // Пришло сообщение из web-socket'а для указанного target-актора.
      // C @-биндингом и unapply() пока есть проблемы. Поэтому достаём JsObject, парсим его, и уже тогда делаем последующие действия.
      case jso: JsObject =>
        try {
          val ans = jso.as[Answer]
          dequeueAnswerReceived()
          if (ans.replyTo.nonEmpty) {
            val sel = context.system.actorSelection(self.path / ans.replyTo.get)
            trace(s"$name: Message received for slave service actor: ${ans.replyTo}...")
            // Пересылаем сообщение целевому актору.
            // Используем send() вместо forward(), чтобы скрыть out от подчинённых акторов.
            sel ! ans
          } else {
            unexpectedReceiver(ans)
          }
        } catch {
          case ex: Exception =>
            warn("JSON received, but cannot parse answer:\n  " + jso, ex)
        }

      // Подчинённый актор хочет отправить js-код для исполнения на клиенте.
      case wsCmd: IWsCmd =>
        wsCmd.sendMode match {
          case CmdSendModes.Async =>
            sendJsCommand(wsCmd)
          case CmdSendModes.Queued =>
            enqueueCommand(wsCmd)
        }

      // Народ требует новых акторов.
      case AddActors(actors) =>
        debug(s"AddActors() with ${actors.size} actors from ${sender()}")
        actors.foreach { props =>
          context.actorOf(props, name = guessChildName())
        }
    }
  }


}

