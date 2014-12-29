package util.adv

import akka.actor.{Props, ActorRef}
import models.adv.MExtServices.MExtService
import models.adv.MExtTarget
import models.adv.js.{EnsureServiceReadyError, EnsureServiceReadySuccess, EnsureServiceReadyAsk}
import play.api.libs.json.JsObject
import util.PlayMacroLogsImpl

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 29.12.14 15:44
 * Description: Актор, обслуживающий один сервис внешнего размещения.
 */
object ExtServiceActor {
  def props(out: ActorRef, service: MExtService, targets0: List[MExtTarget], ctx1: JsObject) = {
    Props(ExtServiceActor(out, service, targets0, ctx1))
  }
}

/** Актор, занимающийся загрузкой карточек в однин рекламный сервис. */
case class ExtServiceActor(out: ActorRef, service: MExtService, targets0: List[MExtTarget], ctx1: JsObject)
  extends EnsureServiceReady
{
  /** Текущее состояние FSM. */
  override protected var _state: FsmState = new DummyState

  override def receive: Receive = PartialFunction.empty

  /** Ресивер для всех состояний. */
  override def allStatesReceiver: Receive = PartialFunction.empty

  override def preStart(): Unit = {
    super.preStart()
    become(new EnsureServiceReadyState)
  }
}


/** Базовый трейт для аддонов этого актора. */
sealed trait ExtServiceActorBase extends FsmActor with PlayMacroLogsImpl {
  val out: ActorRef
  val targets0: List[MExtTarget]
  val ctx1: JsObject
  val service: MExtService
}


/** Поддержка инициализации одного сервиса. */
sealed trait EnsureServiceReady extends ExtServiceActorBase with SioPrJsUtil {

  /** Состояние, когда запускается инициализация API одного сервиса. */
  class EnsureServiceReadyState extends FsmState {
    /** Действия, которые вызываются, когда это состояние выставлено в актор. */
    override def afterBecome(): Unit = {
      super.afterBecome()
      // Сразу отправить в сокет запрос иницализации этого сервиса.
      val ctx11 = service.prepareContext(ctx1)
      val askJson = EnsureServiceReadyAsk(service, ctx11)
      out ! mkJsAsk(askJson)
    }

    override def receiverPart: Receive = {
      case EnsureServiceReadySuccess(_, ctx2, params) =>
        ???
      case EnsureServiceReadyError(_, reason) =>
        ???
    }
  }

}


