package util.adv

import akka.actor.Actor
import controllers.routes
import models.adv.IExtAdvTargetActorArgs
import models.adv.ext.act.ActorPathQs
import models.adv.js.JsCmd
import models.jsm.DomWindowSpecs
import play.api.libs.oauth.RequestToken
import util.PlayMacroLogsImpl
import util.async.{AsyncUtil, FsmActor}
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import util.jsa.JsWindowOpen

import scala.concurrent.Future
import scala.util.{Failure, Success}

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

  override protected var _state: FsmState = ???

  override def receive: Actor.Receive = allStatesReceiver

  /** OAuth1-клиент сервиса. */
  val client = args.target.target.service.oauth1Client

  def domWndTargetName = "popup-authz-" + args.target.target.service.strId

  /**
   * Состояние запроса request token'а.
   * Нужно отправить в твиттер запрос на получение одноразового токена.
   * Одновременно, на стороне юзера открыть попап, который откроет экшен, связанный с этим актором
   * для получения HTTP-редиректа. Ссылка будет сгенерена этим актором.
   *
   */
  class AskRequestTokenState extends FsmState {

    /** Фьючерс с результатом запрос request token'а у твиттера. */
    lazy val reqTokFut = {
      val returnCall = routes.LkAdvExt.oauth1PopupReturnGet(
        tgId = args.target.target.id.get,
        actorInfoQs = ActorPathQs(self.path)
      )
      val returnUrl = returnCall.absoluteURL()(args.request)
      val fut = Future {
        client.retrieveRequestToken(returnUrl)
      }(AsyncUtil.singleThreadIoContext)
      fut.flatMap {
        case Right(reqTok)  => Future successful reqTok
        case Left(ex)       => Future failed ex
      }
    }

    /** Запустить запрос реквест-токена и дожидаться результата. */
    override def afterBecome(): Unit = {
      super.afterBecome()
      reqTokFut onComplete {
        case Success(reqTok)  => self ! SuccessReqTok(reqTok)
        case res              => self ! res
      }
    }

    /** Ожидаем результаты запроса request token'а. */
    override def receiverPart: Receive = {
      // Сервис выдал одноразовый request token. Надо отредиректить юзера, используя этот токен.
      case SuccessReqTok(reqTok) =>
        become(new RemoteUserAuthorizeState(reqTok))

      case Failure(ex) =>
        // TODO Нужно отрендерить ошибку инициализации twitter-сервиса по ws.
        ???
        harakiri()
    }

    /** Статически-типизированный контейнер результата вместо Success(). */
    case class SuccessReqTok(reqTok: RequestToken)
  }


  /**
   * Состояние отправки юзера на авторизацию в сервис и возврата назад.
   * @param reqTok Полученный токен.
   */
  class RemoteUserAuthorizeState(reqTok: RequestToken) extends FsmState {
    /** Действия, которые вызываются, когда это состояние выставлено в актор. */
    override def afterBecome(): Unit = {
      super.afterBecome()
      // TODO Нужно отправить команду для отображения попапа с логином в твиттер (попап в порядке очереди).
      val wndSz = args.target.target.service.oauth1PopupWndSz
      val someFalse = Some(false)
      val jsa = JsWindowOpen(
        url = client.redirectUrl(reqTok.token),
        target = domWndTargetName,
        specs = DomWindowSpecs(
          width       = wndSz.width,
          height      = wndSz.height,
          status      = someFalse,
          menubar     = someFalse,
          directories = someFalse
        )
      )
      val jsCmd = JsCmd(jsa.renderToString(), isPopup = true)
      sendCommand(jsCmd)
    }

    /** Ожидание сигналов от контроллера на предмет возврата юзера с сервиса. */
    override def receiverPart: Receive = ???
  }

}
