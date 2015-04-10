package util.adv

import akka.actor.Props
import models.adv.js.ctx.JsErrorInfo
import models.adv.IOAuth1AdvTargetActorArgs
import models.mext.{MExtService, IExtPostInfo}
import util.PlayMacroLogsImpl
import util.async.FsmActor
import play.api.libs.concurrent.Execution.Implicits.defaultContext

import scala.util.{Failure, Success}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 03.04.15 15:36
 * Description: Актор, занимающийся размещением карточек через уже готовую связь с удаленным сервером.
 * Для размещения используется access_token, полученный от service-актора.
 * Ссылки API берутся из service.oauth1Support.
 */

object OAuth1TargetActor {

  def props(args: IOAuth1AdvTargetActorArgs): Props = {
    Props(OAuth1TargetActor(args))
  }

}


case class OAuth1TargetActor(args: IOAuth1AdvTargetActorArgs)
  extends FsmActor
  with ExtTargetActorUtil
  with ReplyTo
  with MediatorSendCommand
  with PlayMacroLogsImpl
  with CompatWsClient    // TODO
{

  /** Общий ресивер для всех состояний. */
  override def allStatesReceiver: Receive = PartialFunction.empty

  override protected var _state: FsmState = new DummyState

  override def receive: Receive = allStatesReceiver

  def oa1Support = service.oauth1Support.get

  /** Текущий сервис, в котором задействован текущий актор. */
  override def service: MExtService = args.target.target.service

  /** Запуск актора. Выставить исходное состояние. */
  override def preStart(): Unit = {
    super.preStart()
    become(new PublishState)
  }

  /** Состояние публикации одного поста. */
  class PublishState extends FsmState {
    /** При входе в состояние надо запустить постинг с помощью имеющегося access_token'а. */
    override def afterBecome(): Unit = {
      super.afterBecome()
      val mkPostFut = oa1Support.mkPost(
        mad   = args.request.mad,
        acTok = args.accessToken,
        geo   = args.request.producer.geo.point
      )
      renderInProcess()
      mkPostFut onComplete {
        case Success(res) => self ! res
        case other        => self ! other
      }
    }

    override def receiverPart: Receive = {
      // Всё ок
      case newPostInfo: IExtPostInfo =>
        // TODO Нужно ссылку на пост передать в рендер
        renderSuccess()
        harakiri()

      // Ошибка постинга.
      case Failure(ex) =>
        val jsErr = JsErrorInfo(s"${ex.getClass.getSimpleName}: ${ex.getMessage}")
        renderError("e.adv.ext.api", Some(jsErr))
        harakiri()
    }
  }

}
