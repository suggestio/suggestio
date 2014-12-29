package util.adv

import akka.actor.{Props, ActorRef}
import models.adv.MExtServices.MExtService
import models.adv.MExtTarget
import models.adv.js.{ServiceParams, EnsureServiceReadyError, EnsureServiceReadySuccess, EnsureServiceReadyAsk}
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
  extends FsmActor with PlayMacroLogsImpl with SioPrJsUtil
{

  import LOGGER._

  /** Текущее состояние FSM. */
  override protected var _state: FsmState = new DummyState

  override def receive: Receive = PartialFunction.empty

  /** Ресивер для всех состояний. */
  override def allStatesReceiver: Receive = PartialFunction.empty

  override def preStart(): Unit = {
    super.preStart()
    become(new EnsureServiceReadyState)
  }

  protected def harakiri(): Unit = {
    context stop self
  }

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
      case EnsureServiceReadySuccess((_, ctx2, params)) =>
        val nextState = if (params.picture.needStorage) {
          new HasPictureStorageState(ctx2, params)
        } else {
          // TODO Генерить абсолютную ссылку на отрендеренную карточку-картинку.
          new WallPostState(???)
        }
        become(nextState)

      case EnsureServiceReadyError((_, reason)) =>
        error(name + ": JS failed to ensureServiceReady: " + reason)
        harakiri()
    }
  }


  /** Если требуется отдельное обслуживание хранилища картинок, то в этом состоянии узнаём,
    * существует ли необходимое хранилище на сервисе? */
  class HasPictureStorageState(ctx2: JsObject, params: ServiceParams) extends FsmState {
    override def receiverPart: Receive = ???
  }


  /** Состояние постинга на стену. */
  class WallPostState(imgId: String) extends FsmState {
    override def receiverPart: Receive = ???
  }

}


