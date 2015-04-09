package util.adv

import akka.actor.Props
import models.adv.IOAuth1AdvTargetActorArgs
import util.PlayMacroLogsImpl
import util.async.FsmActor

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
  with MediatorSendCommand
  with PlayMacroLogsImpl
  with CompatWsClient    // TODO
{

  /** Общий ресивер для всех состояний. */
  override def allStatesReceiver: Receive = PartialFunction.empty

  override protected var _state: FsmState = new DummyState

  override def receive: Receive = allStatesReceiver

  /** OAuth1-клиент сервиса. */
  //val client = args.target.target.service.oauth1Support.get.client

  /** Запуск актора. Выставить исходное состояние. */
  override def preStart(): Unit = {
    super.preStart()
    become(???)
  }

  /** Состояние публикации одного поста. */
  class PublishState extends FsmState {
    // При входе в состояние надо запустить постинг с помощью имеющегося access_token'а.
    override def afterBecome(): Unit = {
      super.afterBecome()
    }

    override def receiverPart: Receive = {
      ???
    }
  }

}
