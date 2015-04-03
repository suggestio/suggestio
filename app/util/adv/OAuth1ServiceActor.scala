package util.adv

import controllers.routes
import models.adv.IExtAdvServiceActorArgs
import models.adv.ext.act.{OAuthVerifier, ActorPathQs}
import models.adv.js.JsCmd
import models.jsm.DomWindowSpecs
import play.api.libs.oauth.RequestToken
import util.PlayMacroLogsImpl
import util.async.{AsyncUtil, FsmActor}
import util.jsa.JsWindowOpen

import scala.concurrent.Future
import scala.util.{Success, Failure}
import play.api.libs.concurrent.Execution.Implicits.defaultContext

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 03.04.15 21:23
 * Description: service-level actor для подготовки OAuth1 к работе в рамках одного oauth1-сервиса.
 * Актор занимается инициализацией состояния OAuth1-контекста, а именно получением access_token'а.
 * Токен хранится в куках у клиента, но нужно производить проверку его.
 */
object OAuth1ServiceActor extends IServiceActorCompanion


case class OAuth1ServiceActor(args: IExtAdvServiceActorArgs)
  extends FsmActor
  with MediatorSendCommand
  with PlayMacroLogsImpl
{

  import LOGGER._

  /** Общий ресивер для всех состояний. */
  override def allStatesReceiver: Receive = PartialFunction.empty

  override protected var _state: FsmState = new DummyState

  override def receive: Receive = allStatesReceiver

  /** OAuth1-клиент сервиса. */
  val client = args.service.oauth1Client

  /** Имя js-попапа, в рамках которого происходит авторизация пользователя сервисом. */
  def domWndTargetName = "popup-authz-" + args.service.strId

  /** Запуск актора. Выставить исходное состояние. */
  override def preStart(): Unit = {
    super.preStart()
    become(new AskRequestTokenState)
  }


  /**
   * Состояние запроса request token'а.
   * Нужно отправить в твиттер запрос на получение одноразового токена.
   * Одновременно, на стороне юзера открыть попап, который откроет экшен, связанный с этим актором
   * для получения HTTP-редиректа. Ссылка будет сгенерена этим актором.
   */
  class AskRequestTokenState extends FsmState {

    /** Фьючерс с результатом запрос request token'а у твиттера. */
    lazy val reqTokFut = {
      val returnCall = routes.LkAdvExt.oauth1PopupReturnGet(
        adnId = args.request.producerId,
        actorInfoQs = ActorPathQs(self.path)
      )
      val returnUrl = "http://127.0.0.1:9000" + returnCall.url    // TODO Брать URL_PREFIX из конфигов или откуда-нить ещё
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
        case Success(reqTok)  => self ! SuccessToken(reqTok)
        case res              => self ! res
      }
    }

    /** Ожидаем результаты запроса request token'а. */
    override def receiverPart: Receive = {
      // Сервис выдал одноразовый request token. Надо отредиректить юзера, используя этот токен.
      case SuccessToken(reqTok) =>
        trace("Got request token: " + reqTok.token)
        become(new RemoteUserAuthorizeState(reqTok))

      case Failure(ex) =>
        error("Failed to ask request token", ex)
        // TODO Нужно отрендерить ошибку инициализации twitter-сервиса по ws.
        harakiri()
    }
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
      val wndSz = args.service.oauth1PopupWndSz
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
    override def receiverPart: Receive = {
      case OAuthVerifier(Some(verifier)) =>
        trace("Received oauth verifier: " + verifier)
        become(new RetrieveAccessTokenState(reqTok, verifier))
      case other =>
        warn("msg rcvrd " + other)
    }
  }


  class RetrieveAccessTokenState(reqTok: RequestToken, verifier: String) extends FsmState {

    /** Действия, которые вызываются, когда это состояние выставлено в актор. */
    override def afterBecome(): Unit = {
      super.afterBecome()
      // TODO Дедублицировать этот код с кодом выше.
      val acTokFut = Future {
        client.retrieveAccessToken(reqTok, verifier)
      }(AsyncUtil.singleThreadIoContext)
        .flatMap {
          case Right(acTok)   => Future successful acTok
          case Left(ex)       => Future failed ex
        }
      acTokFut onComplete {
        case Success(acTok) => self ! SuccessToken(acTok)
        case other => self ! other
      }
    }

    override def receiverPart: Receive = {
      case resp =>
        trace("received resp: " + resp)
    }

  }

  /** Статически-типизированный контейнер токена-результата вместо Success(). */
  case class SuccessToken(token: RequestToken)
}
