package util.adv

import _root_.util.{PlayMacroLogsI, PlayLazyMacroLogsImpl}
import _root_.util.ws.SubscribeToWsDispatcher
import _root_.util.SiowebEsUtil.client
import akka.actor.{Actor, ActorRef, Props}
import models.adv.{MExtAdvContext, MExtServices, MExtTarget}
import models.adv.js.{EnsureReadyError, EnsureReadySuccess, AskBuilder, EnsureReadyAsk}
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
  def props(out: ActorRef, eactx: MExtAdvContext) = Props(new ExtAdvWsActor(out, eactx))
}


/** ws-актор, готовый к использованию websocket api. */
case class ExtAdvWsActor(out: ActorRef, eactx: MExtAdvContext)
  extends ExtAdvWsActorBase
  with EnsureReady
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
    def receiver = receiverPart orElse superReceiver
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
  val eactx: MExtAdvContext
}


/** Поддержка диалога ensureReady.  */
sealed trait EnsureReady extends ExtAdvWsActorBase with SuperviseServiceActors with SioPrJsUtil {

  import LOGGER._

  /** Начальное состояние, передаваемое в prepareReady.
    * По мере необходимости, сюда можно добавлять новые поля. */
  def ctx0 = JsObject(Nil)

  /** Состояние диалога на этапе начальной инициализации. */
  class EnsureReadyState extends FsmState {

    /** Фьючерс с целями для размещения рекламной карточки. Результаты работы понадобятся на следующем шаге. */
    val targetsFut = MExtTarget.multiGet(eactx.qs.targetIds)

    /** Действия, которые вызываются, когда это состояние выставлено в актор. */
    override def afterBecome(): Unit = {
      super.afterBecome()
      out ! mkJsAsk( EnsureReadyAsk(ctx0) )
    }

    override def receiverPart: Receive = {
      // Положительный ответ на ensureReady
      case EnsureReadySuccess(ctx1) =>
        trace(s"$name: success. New context = $ctx1")
        become(new WaitForTargetsState(targetsFut, ctx1))

      // Проблемы при инициализации
      case EnsureReadyError(reason) =>
        error(s"$name: js error: $reason")
        become(new DummyState)
    }
  }


  /**
   * Состояние ожидания асинхронных данных по целям для размещения, запущенных в предыдущем состоянии.
   * @param targetsFut Фьючерс с будущими целями.
   */
  class WaitForTargetsState(targetsFut: Future[Seq[MExtTarget]], ctx1: JsObject) extends FsmState {
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
        become(new SuperviseServiceActorsState(targets, ctx1))
      case TargetsFailed(ex) =>
        error(s"$name: Failed to aquire targers", ex)
        become(new DummyState)
    }

    case class TargetsReady(targets: List[MExtTarget])
    case class TargetsFailed(ex: Throwable)
  }

}


/** Поддержка инициализации сервисов и диспатчинга их по акторам. */
sealed trait SuperviseServiceActors extends ExtAdvWsActorBase {

  import LOGGER._

  /**
   * Состояние перехода на обработку целей размещения.
   * @param targets Цели, полученные из хранилища.
   * @param ctx1 Состояние после начальной инициализации.
   */
  class SuperviseServiceActorsState(targets: List[MExtTarget], ctx1: JsObject) extends FsmState {
    override def afterBecome(): Unit = {
      super.afterBecome()
      // Сразу запустить паралельных акторов, которые паралельно занимаются обслуживанием каждого сервиса.
      targets.groupBy(_.service).foreach {
        case (service, serviceTargets) =>
          val props = ExtServiceActor.props(out, service, serviceTargets, ctx1, eactx)
          context.actorOf(props, name = service.strId)
      }
    }

    override def receiverPart: Receive = {
      // Пришло сообщение от js. Там в аргументах внутри должно быть поле service, которое идентифицирует дочернего актора.
      case jso: JsObject =>
        val srvOpt = MExtServices.maybeWithName( (jso \ "args" \ "service").toString() )
        srvOpt match {
          case Some(srv) =>
            val sel = context.system.actorSelection(self.path / srv.strId)
            sel forward jso
          case None =>
            warn(s"$name: Dropping unexpected message: " + jso)
        }
    }
  }
}

