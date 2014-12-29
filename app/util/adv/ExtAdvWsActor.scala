package util.adv

import _root_.util.PlayLazyMacroLogsImpl
import _root_.util.ws.SubscribeToWsDispatcher
import akka.actor.{Actor, ActorRef, Props}
import models.adv.MExtAdvQs
import models.adv.js.{EnsureReadyError, EnsureReadySuccess, AskBuilder, EnsureReadyAsk}
import play.api.libs.json._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 26.12.14 16:24
 * Description: Утиль и код актора, который занимается общением с js api размещения рекламных карточек на клиенте.
 */
object ExtAdvWsActor {

  def props(out: ActorRef, args: MExtAdvQs) = Props(new ExtAdvWsActor(out, args))

}

/** ws-актор, готовый к использованию websocket api. */
case class ExtAdvWsActor(out: ActorRef, args: MExtAdvQs)
  extends ExtAdvWsActorBase
  with EnsureReady
{
  override def wsId: String = args.wsId

  override protected var _state: FsmState = new EnsureReadyState

  override def allStatesReceiver: Receive = PartialFunction.empty
  override def receive: Actor.Receive = allStatesReceiver
  
}


/** Базовый трейт для запиливания реализации актора и его аддонов */
trait ExtAdvWsActorBase extends Actor with SubscribeToWsDispatcher with PlayLazyMacroLogsImpl {

  import LOGGER._

  def out: ActorRef
  def args: MExtAdvQs

  def sioPrJs = "SioPR"

  /** Текущее состояние. */
  protected var _state: FsmState

  /** Ресивер для всех состояний. */
  def allStatesReceiver: Receive

  /**
   * Переключение на новое состояние. Старое состояние будет отброшено.
   * @param nextState Новое состояние.
   */
  def become(nextState: FsmState): Unit = {
    trace(s"become(): fsm mode switch ${_state} -> $nextState")
    _state.beforeUnbecome()
    _state = nextState
    context.become(_state.receiver, discardOld = true)
    _state.afterBecome()
  }

  /** Интерфейс одного состояния. */
  trait FsmState {
    def name = getClass.getSimpleName
    def logPrefix = name + ": "
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

  /**
   * Сборка JSON запроса на основе указанных данных.
   * @param data Данные запроса. Обычно js-генератор.
   * @param askType Тип запроса. Обычно "js".
   * @return JSON.
   */
  def mkJsAsk(data: AskBuilder, askType: String = "js"): JsObject = {
    JsObject(Seq(
      "type" -> JsString(askType),
      "data" -> JsString(data.js)
    ))
  }

}


/** Поддержка диалога ensureReady.  */
trait EnsureReady extends ExtAdvWsActorBase {

  import LOGGER._

  /** Начальное состояние, передаваемое в prepareReady.
    * По мере необходимости, сюда можно добавлять новые поля. */
  def ctx0 = JsObject(Nil)
  
  protected var _ctx1: JsValue = JsNull

  /** Состояние диалога на этапе начальной инициализации. */
  class EnsureReadyState extends FsmState {
    /** Действия, которые вызываются, когда это состояние выставлено в актор. */
    override def afterBecome(): Unit = {
      super.afterBecome()
      out ! mkJsAsk( EnsureReadyAsk(ctx0) )
    }

    override def receiverPart: Receive = {
      // Ответ на ensureReady
      case EnsureReadySuccess(ctx1) =>
        _ctx1 = ctx1
        // TODO Перейти на следующее состояние
        ???
      case EnsureReadyError(reason) =>
        error("js error:" + reason)
        // TODO Нужно включить сценарий
        ???
    }
  }

}
