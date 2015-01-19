package util.adv

import _root_.util.{PlayMacroLogsI, PlayLazyMacroLogsImpl}
import _root_.util.ws.SubscribeToWsDispatcher
import _root_.util.SiowebEsUtil.client
import akka.actor.{Actor, ActorRef, Props}
import models.adv._
import models.adv.js.ctx.MJsCtx
import models.adv.js._
import play.api.libs.json._
import play.api.libs.concurrent.Execution.Implicits.defaultContext

import scala.concurrent.Future
import scala.util.{Failure, Success}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 26.12.14 16:24
 * Description: Утиль и код актора, который занимается общением с js api размещения рекламных карточек на клиенте.
 */
object ExtAdvWsActor {
  def props(out: ActorRef, eactx: MExtAdvArgsT) = Props(new ExtAdvWsActor(out, eactx))
}


/** ws-актор, готовый к использованию websocket api. */
case class ExtAdvWsActor(out: ActorRef, eactx: MExtAdvArgsT)
  extends ExtAdvWsActorBase
  with EnsureReadyAddon
{
  override def wsId: String = eactx.qs.wsId

  override def allStatesReceiver: Receive = PartialFunction.empty
  override def receive: Actor.Receive = allStatesReceiver

  /** Текущее состояние FSM хранится здесь. */
  override protected var _state: FsmState = new DummyState

  override def preStart(): Unit = {
    super.preStart()
    become(new EnsureReadyState)
  }

}


// TODO Вынести FsmActor в другое место.
/** Утиль для построения FSM-актора. */
trait FsmActor extends Actor with PlayMacroLogsI {

  /** Текущее состояние. */
  protected var _state: FsmState

  /** Ресивер для всех состояний. */
  def allStatesReceiver: Receive

  /**
   * Переключение на новое состояние. Старое состояние будет отброшено.
   * @param nextState Новое состояние.
   */
  def become(nextState: FsmState): Unit = {
    LOGGER.trace(s"become(): fsm mode switch ${_state} -> $nextState")
    _state.beforeUnbecome()
    _state = nextState
    context.become(_state.receiver, discardOld = true)
    _state.afterBecome()
  }

  /** Ресивер, добавляемый к конец reveive() для всех состояний, чтобы выводить в логи сообщения,
    * которые не были отработаны актором. */
  protected val unexpectedReceiver: Receive = {
    case other =>
      LOGGER.warn(s"${_state.name} Unexpected message dropped [${other.getClass.getName}]:\n  $other")
  }

  /** Интерфейс одного состояния. */
  trait FsmState {
    def name = getClass.getSimpleName
    def receiverPart: Receive
    def superReceiver = allStatesReceiver
    def maybeSuperReceive(msg: Any): Unit = {
      val sr = superReceiver
      if (sr isDefinedAt msg)
        sr(msg)
    }
    def receiver = receiverPart orElse superReceiver orElse unexpectedReceiver
    override def toString: String = name

    /** Действия, которые вызываются, когда это состояние выставлено в актор. */
    def afterBecome() {}

    /** Действия, которые вызываются, перед тем как это состояние слетает из актора. */
    def beforeUnbecome() {}
  }

  /** Состояние-заглушка. Не делает ровным счётом ничего. */
  class DummyState extends FsmState {
    override def receiverPart: Receive = PartialFunction.empty
  }

}


/** Утиль взаимодействия с js. */
trait SioPrJsUtil {

  /**
   * Сборка JSON запроса на основе указанных данных.
   * @param data Данные запроса. Обычно js-генератор.
   * @param askType Тип запроса. Обычно "js" (по умоляанию).
   * @return JSON.
   */
  def mkJsAsk(data: AskBuilder, askType: String = "js"): JsObject = {
    JsObject(Seq(
      "type" -> JsString(askType),
      "data" -> JsString(data.js)
    ))
  }

}


/** Базовый трейт для запиливания реализации актора и его аддонов */
sealed trait ExtAdvWsActorBase extends FsmActor with SubscribeToWsDispatcher with PlayLazyMacroLogsImpl {

  val out: ActorRef
  val eactx: MExtAdvArgsT

}


/** Поддержка диалога ensureReady.  */
sealed trait EnsureReadyAddon extends ExtAdvWsActorBase with SuperviseServiceActors with SioPrJsUtil {

  import LOGGER._

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
      out ! mkJsAsk( EnsureReadyAsk(ctx0) )
      targetsFut  // Запускаем на исполнение lazy val future
    }

    override def receiverPart: Receive = {
      // Пришел какой-то ответ на ensureReady
      case Answer(status, replyTo, mctx1) if replyTo == EnsureReady.action =>
        val nextState = if (status.isSuccess) {
          // Инициализация выполнена.
          trace(s"$name: success. New context = ${mctx1.json}")
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

}


/** Поддержка инициализации сервисов и диспатчинга их по акторам. */
sealed trait SuperviseServiceActors extends ExtAdvWsActorBase { actor =>

  import LOGGER._

  /**
   * Состояние перехода на обработку целей размещения.
   * @param targets Цели, полученные из хранилища.
   * @param mctx1 Состояние после начальной инициализации.
   */
  class SuperviseServiceActorsState(targets: ActorTargets_t, mctx1: MJsCtx) extends FsmState {
    override def afterBecome(): Unit = {
      super.afterBecome()
      // Сразу запустить паралельных акторов, которые паралельно занимаются обслуживанием каждого сервиса.
      targets.groupBy(_.target.service).foreach {
        case (_service, serviceTargets) =>
          val args = new MExtServiceAdvArgsT with MExtAdvArgsWrapperT {
            override def out                = actor.out
            override def mctx0              = mctx1
            override def service            = _service
            override def targets0           = serviceTargets
            override def _eaArgsUnderlying  = eactx
          }
          val props = ExtServiceActor.props(args)
          // TODO Нужно придумать более безопасный для akka идентификатор: uuid+hex? uuid+base64+urlsafe?
          val actorName = _service.strId
          context.actorOf(props, name = actorName)
      }
    }

    override def receiverPart: Receive = {
      // Пришло сообщение от js для другого service-актора.
      case sa @ ServiceAnswer(status, service, replyTo, ctx2) =>
        val sel = context.system.actorSelection(self.path / service.strId)
        trace(s"$name: Message received for slave service actor: $service. Forwarding message to actor $sel")
        sel forward sa
    }
  }
}

