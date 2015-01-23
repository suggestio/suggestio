package util.adv

import _root_.util.PlayMacroLogsImpl
import _root_.util.async.FsmActor
import _root_.util.ws.SubscribeToWsDispatcher
import _root_.util.SiowebEsUtil.client
import akka.actor.{Actor, ActorRef, Props}
import io.suggest.util.UrlUtil
import models.adv._
import models.adv.js.ctx.MJsCtx
import models.adv.js._
import play.api.libs.json._
import play.api.libs.concurrent.Execution.Implicits.defaultContext

import scala.collection.immutable.Queue
import scala.concurrent.Future
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
    Props(new ExtAdvWsActor(out, eactx))
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
  protected var _queue = Queue[JsCommand]()


  /** Сериализация и отправка одной js-команды в веб-сокет. */
  def sendJsCommand(jsCmd: JsCommand): Unit = {
    out ! Json.toJson(jsCmd)
  }

  /** Получена js-команда от какого-то актора. Нужно в зависимости от ситуации отправить её в очередь
    * или же отправить немедлено, выставив флаг блокировки. */
  def enqueueCommand(ask: JsCommand): Unit = {
    if (_jsAskLock) {
      // Клиент отрабатывает другой ask. Значит отправить в очередь.
      trace("asking locked. 1 command queued.")
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
          become(new EnsureReadyState(targets))
        }
      case TargetsFailed(ex) =>
        error(s"$name: Failed to aquire targers", ex)
        become(new DummyState)
    }

    case class TargetsReady(targets: ActorTargets_t)
    case class TargetsFailed(ex: Throwable)
  }



  /** Состояние диалога на этапе начальной инициализации. */
  class EnsureReadyState(targets: ActorTargets_t) extends FsmState {

    /** Действия, которые вызываются, когда это состояние выставлено в актор. */
    override def afterBecome(): Unit = {
      super.afterBecome()
      // Отправить запрос на подготовку к работе.
      val mctx0 = MJsCtx(
        domain = targets
          .iterator
          .map { tg => UrlUtil.url2dkey(tg.target.url) }
          .toSet
          .toSeq
      )
      val cmd = JsCommand(EnsureReadyAsk(mctx0), CmdSendModes.Async)
      sendJsCommand(cmd)
    }

    override def receiverPart: Receive = {
      // Пришел какой-то ответ на ensureReady
      case Answer(replyToOpt, mctx1) if replyToOpt.isEmpty =>
        val nextState = if (mctx1.status contains AnswerStatuses.Success) {
          // Инициализация выполнена.
          trace(s"$name: success. New context = $mctx1")
          new SuperviseServiceActorsState(targets, mctx1)
        } else {
          // Проблемы при инициализации
          error(s"$name: js returned error")    // TODO Выводить ошибку из контекста.
          new DummyState
        }
        become(nextState)
    }
  }


  /**
   * Состояние перехода на обработку целей размещения.
   * @param targets Цели, полученные из хранилища.
   * @param mctx1 Состояние после начальной инициализации.
   */
  class SuperviseServiceActorsState(targets: ActorTargets_t, mctx1: MJsCtx) extends FsmState {
    override def afterBecome(): Unit = {
      super.afterBecome()
      // Запускаем всех акторов для всех таргетов. Они сразу заполнят всю очередь запросами.
      val rnd = new Random()
      targets.foreach { tg =>
        val args = new IExtAdvTargetActorArgs with IExtAdvArgsWrapperT {
          override def mctx0    = mctx1
          override def target   = tg
          override def _eaArgsUnderlying = eactx
        }
        val props = ExtTargetActor.props(args)
        // Придумываем рандомный идентификатор для нового актора. По этому id будут роутится js-ответы.
        val actorName = Math.abs(rnd.nextLong()).toString
        context.actorOf(props, name = actorName)
      }
    }

    /** Обработка входящих сообщений: как от js, так и от акторов. */
    override def receiverPart: Receive = {
      // Пришло сообщение из web-socket'а для указанного target-актора.
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
      case jsCmd: JsCommand =>
        jsCmd.sendMode match {
          case CmdSendModes.Async =>
            sendJsCommand(jsCmd)
          case CmdSendModes.Queued =>
            enqueueCommand(jsCmd)
        }
    }
  }


}

