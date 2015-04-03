package util.adv

import models.adv.IExtAdvTargetActorArgs
import util.PlayMacroLogsImpl
import util.async.FsmActor

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 03.04.15 15:36
 * Description: Актор, занимающийся взаимодействие с удаленным сервисом через OAuth1 API.
 * В частоности, это нужно для взаимодействия с твиттером.
 * Актор поддерживает связь с юзером через ws, js-команды и через контроллер, пробрасывающий HTTP-запросы этому актору.
 *
 * Работа с request token'ами реализована как stateful внутри актора.
 * access token шифруется и хранится у юзера в кукисе.
 * При следующем запросе access token будет расшифрован из кукиса. Таким образом, можно иметь максимум
 * один уже готовый access token.
 * @see [[https://www.playframework.com/documentation/2.4.x/ScalaOAuth]]
 */
class OAuth1TargetActor(val args: IExtAdvTargetActorArgs)
  extends FsmActor
  with MediatorSendCommand
  with PlayMacroLogsImpl
{

  /** Общий ресивер для всех состояний. */
  override def allStatesReceiver: Receive = PartialFunction.empty

  override protected var _state: FsmState = new DummyState

  override def receive: Receive = allStatesReceiver

  /** OAuth1-клиент сервиса. */
  val client = args.target.target.service.oauth1Client

  /** Имя js-попапа, в рамках которого происходит авторизация пользователя сервисом. */
  def domWndTargetName = "popup-authz-" + args.target.target.service.strId

  /** Запуск актора. Выставить исходное состояние. */
  override def preStart(): Unit = {
    super.preStart()
    become(???)
  }

}
