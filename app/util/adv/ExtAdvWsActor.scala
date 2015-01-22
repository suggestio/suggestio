package util.adv

import _root_.util.PlayMacroLogsImpl
import _root_.util.acl.RequestWithAdAndProducer
import _root_.util.async.FsmActor
import _root_.util.ws.SubscribeToWsDispatcher
import _root_.util.SiowebEsUtil.client
import akka.actor.{Actor, ActorRef, Props}
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
  def props(out: ActorRef, eactx: MExtAdvArgsT): Props = {
    Props(new ExtAdvWsActor(out, eactx))
  }

}


/** ws-актор, готовый к использованию websocket api. */
case class ExtAdvWsActor(out: ActorRef, eactx: MExtAdvArgsT)
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
      _queue = _queue.enqueue(ask)
    } else {
      // Клиент свободен. Считаем, что очередь пуста. Сразу отправляем, выставляя флаг.
      sendJsCommand(ask)
      _jsAskLock = true
    }
  }

  /** Необходимо пропедалировать очередь команд вперёд в связи с получением какого-то ответа от js. */
  def dequeueAnswerReceived(): Unit = {
    if (_queue.isEmpty) {
      // Очередь пуста. Снимает флаг блокировки.
      _jsAskLock = false
    } else {
      // В очереди ещё есть запросы к js -- отправляем следующий запрос.
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
    become(new EnsureReadyState)
  }



  /** Начальное состояние, передаваемое в prepareReady.
    * По мере необходимости, сюда можно добавлять новые поля. */
  def ctx0 = JsObject(Nil)


  /** Состояние диалога на этапе начальной инициализации. */
  class EnsureReadyState extends FsmState {

    /** Фьючерс с целями для размещения рекламной карточки. Результаты работы понадобятся на следующем шаге. */
    lazy val targetsFut: Future[ActorTargets_t] = {
      val ids = eactx.qs.targets.iterator.map(_.targetId)
      val targetsFut = MExtTarget.multiGet(ids)
      val targetsMap = eactx.qs
        .targets
        .iterator
        .map { info => info.targetId -> info }
        .toMap
      targetsFut map { targets =>
        targets.iterator
          .flatMap { target =>
            target.id
              .flatMap(targetsMap.get)
              .map { info => MExtTargetInfoFull(target, info.returnTo) }
          }
          .toList
      }
    }

    /** Действия, которые вызываются, когда это состояние выставлено в актор. */
    override def afterBecome(): Unit = {
      super.afterBecome()
      // Отправить запрос на подготовку к работе.
      val cmd = JsCommand(EnsureReadyAsk(ctx0), CmdSendModes.Async)
      sendJsCommand(cmd)
      // Запускаем на исполнение lazy val future. Это сократит временные издержки.
      targetsFut
    }

    override def receiverPart: Receive = {
      // Пришел какой-то ответ на ensureReady
      case Answer(replyToOpt, mctx1) if replyToOpt.isEmpty =>
        val nextState = if (mctx1.status contains AnswerStatuses.Success) {
          // Инициализация выполнена.
          trace(s"$name: success. New context = $mctx1")
          new WaitForTargetsState(targetsFut, mctx1)
        } else {
          // Проблемы при инициализации
          error(s"$name: js returned error")    // TODO Выводить ошибку из контекста.
          new DummyState
        }
        become(nextState)
    }
  }


  /**
   * Состояние ожидания асинхронных данных по целям для размещения, запущенных в предыдущем состоянии.
   * @param targetsFut Фьючерс с будущими целями.
   */
  class WaitForTargetsState(targetsFut: Future[ActorTargets_t], mctx1: MJsCtx) extends FsmState {
    override def afterBecome(): Unit = {
      super.afterBecome()
      // Повесить callback'и на фьючерс с таргетами.
      targetsFut onComplete {
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
          become(new SuperviseServiceActorsState(targets, mctx1))
        }
      case TargetsFailed(ex) =>
        error(s"$name: Failed to aquire targers", ex)
        become(new DummyState)
    }

    case class TargetsReady(targets: ActorTargets_t)
    case class TargetsFailed(ex: Throwable)
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
        val args = new MExtAdvTargetActorArgs with MExtAdvArgsWrapperT {
          override def service  = tg.target.service
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
      case sa @ Answer(replyTo, ctx2) if replyTo.nonEmpty =>
        dequeueAnswerReceived()
        val sel = context.system.actorSelection(self.path / replyTo.get)
        trace(s"$name: Message received for slave service actor: $replyTo...")
        // Пересылаем сообщение целевому актору.
        // Используем send() вместо forward(), чтобы скрыть out от подчинённых акторов.
        sel ! sa

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

