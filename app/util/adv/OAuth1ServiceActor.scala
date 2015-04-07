package util.adv

import java.io.{ByteArrayOutputStream, ByteArrayInputStream}

import controllers.routes
import models.adv._
import models.adv.ext.act.{ExtServiceActorEnv, ExtActorEnv, OAuthVerifier, ActorPathQs}
import models.adv.js.{GetStorageCmd, SetStorageCmd, JsCmd}
import models.event.ErrorInfo
import models.jsm.DomWindowSpecs
import models.ls.LsOAuth1Info
import models.sec.MAsymKey
import oauth.signpost.exception.OAuthException
import play.api.libs.json.Json
import play.api.libs.oauth.RequestToken
import util.PlayMacroLogsImpl
import util.async.{AsyncUtil, FsmActor}
import util.jsa.JsWindowOpen
import util.secure.PgpUtil

import scala.concurrent.Future
import scala.util.{Success, Failure}
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import util.SiowebEsUtil.{client => esClient}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 03.04.15 21:23
 * Description: service-level actor для подготовки OAuth1 к работе в рамках одного oauth1-сервиса.
 * Актор занимается инициализацией состояния OAuth1-контекста, а именно получением access_token'а.
 * TODO Ранее полученный токен хранится в куках у клиента, но нужно производить проверку его.
 *
 * @see [[https://www.playframework.com/documentation/2.4.x/ScalaOAuth]]
 */
object OAuth1ServiceActor extends IServiceActorCompanion

case class OAuth1ServiceActor(args: IExtAdvServiceActorArgs)
  extends FsmActor
  with ReplyTo
  with ExtServiceActorEnv
  with MediatorSendCommand
  with PlayMacroLogsImpl
  with ExtServiceActorUtil
{

  import LOGGER._

  /** Общий ресивер для всех состояний. */
  override def allStatesReceiver: Receive = PartialFunction.empty

  override protected var _state: FsmState = new DummyState

  override def receive: Receive = allStatesReceiver

  /** OAuth1-клиент сервиса. */
  val client = args.service.oauth1Client

  /** Ключ шифрования-дешифрования для хранения данных в localStorage. */
  lazy val lsCryptoKey = MAsymKey.getById(PgpUtil.LOCAL_STOR_KEY_ID)
    .map(_.get)

  /** Ключ для хранения секретов access_token'а, относящихся к юзеру. */
  lazy val lsValueKey = s"adv.ext.svc.${args.service.strId}.access.${args.request.pwOpt.fold("__ANON__")(_.personId)}"

  /** Имя js-попапа, в рамках которого происходит авторизация пользователя сервисом. */
  def domWndTargetName = "popup-authz-" + args.service.strId

  /** Запуск актора. Выставить исходное состояние. */
  override def preStart(): Unit = {
    super.preStart()
    become(new AskRequestTokenState(userHaveInvalTok = false))
  }


  /** Псевдоасинхронный запрос какого-то токена у удаленного сервиса. */
  protected def askToken(f: => Either[OAuthException, RequestToken]): Unit = {
    Future(f)(AsyncUtil.singleThreadIoContext)
      // Завернуть результат в правильный Future.
      .flatMap {
        case Right(reqTok)  => Future successful reqTok
        case Left(ex)       => Future failed ex
      }
      // Сообщить текущему актору о результатах.
      .onComplete {
        case Success(res)   => self ! SuccessToken(res)
        case failure        => self ! failure
      }
  }

  /**
   * Рендер ошибки, возникшей в ходе инициализации сервиса.
   * @param ex Исключение. Как правило, это экземпляр OAuthException.
   */
  protected def renderInitFailed(ex: Throwable): Unit = {
    serviceInitFailedRender(
      errors = Seq(ErrorInfo(
        msg = "e.adv.ext.api.init",
        info = Some(s"${ex.getClass.getSimpleName}: ${ex.getMessage}")
      ))
    )
  }


  /** Запросить ранее сохраненный access_token из браузера клиента. */
  class GetSavedAcTokFromUserState extends FsmState {
    override def afterBecome(): Unit = {
      super.afterBecome()
      val jsCmd = GetStorageCmd(
        key     = lsValueKey,
        replyTo = Some(replyTo)
      )
      sendCommand(jsCmd)
      // TODO Нужен timeout на случай проблем
    }

    override def receiverPart: Receive = {
      ???
    }
  }


  /**
   * Состояние запроса request token'а.
   * Нужно отправить в твиттер запрос на получение одноразового токена.
   * Одновременно, на стороне юзера открыть попап, который откроет экшен, связанный с этим актором
   * для получения HTTP-редиректа. Ссылка будет сгенерена этим актором.
   */
  class AskRequestTokenState(userHaveInvalTok: Boolean) extends FsmState {

    /** Запустить запрос реквест-токена и дожидаться результата. */
    override def afterBecome(): Unit = {
      super.afterBecome()
      askToken {
        val returnCall = routes.LkAdvExt.oauth1PopupReturnGet(
          adnId = args.request.producerId,
          actorInfoQs = ActorPathQs(self.path)
        )
        val returnUrl = "http://127.0.0.1:9000" + returnCall.url // TODO Брать URL_PREFIX из конфигов или откуда-нить ещё
        client.retrieveRequestToken(returnUrl)
      }
    }

    /** Ожидаем результаты запроса request token'а. */
    override def receiverPart: Receive = {
      // Сервис выдал одноразовый request token. Надо отредиректить юзера, используя этот токен.
      case SuccessToken(reqTok) =>
        trace("Got request token: " + reqTok.token)
        become(new RemoteUserAuthorizeState(reqTok, userHaveInvalTok))

      case Failure(ex) =>
        error("Failed to ask request token", ex)
        // Отрендерить ошибку инициализации twitter-сервиса по ws.
        renderInitFailed(ex)
        // Пока возможностей типа "попробовать снова" нет, поэтому сразу завершаемся.
        harakiri()
    }
  }


  /**
   * Состояние отправки юзера на авторизацию в сервис и возврата назад.
   * @param reqTok Полученный токен.
   */
  class RemoteUserAuthorizeState(reqTok: RequestToken, userHaveInvalTok: Boolean) extends FsmState {
    /** Действия, которые вызываются, когда это состояние выставлено в актор. */
    override def afterBecome(): Unit = {
      super.afterBecome()
      // Нужно отправить команду для отображения попапа с логином в твиттер (попап в порядке очереди).
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
        // TODO Вывалить ошибку инициализации юзеру
        if (userHaveInvalTok) {
          // Стереть текущий токен из localStorage юзера.
          val jsCmd = SetStorageCmd(key = lsValueKey, value = None)
          sendCommand(jsCmd)
        }
    }
  }


  /** Состояние получения access_token'а. */
  class RetrieveAccessTokenState(reqTok: RequestToken, verifier: String) extends FsmState {

    /** Сохранение access_token'а на клиенте. */
    def saveAcTokOnClient(acTok: RequestToken): Unit = {
      args.request.pwOpt.foreach { pw =>
        lsCryptoKey onSuccess { case pgpKey =>
          // Сериализовать accessToken вместе с метаданными
          val info = LsOAuth1Info(acTok, pw.personId, timestamp = System.currentTimeMillis())
          val json = Json.toJson(info).toString()
          // Зашифровать всё с помощью PGP.
          val baos = new ByteArrayOutputStream(1024)
          PgpUtil.encryptForSelf(
            data = new ByteArrayInputStream(json.getBytes()),
            key  = pgpKey,
            out  = baos
          )
          // Сохранить шифротекст на клиенте.
          val jsCmd = SetStorageCmd(
            key   = lsValueKey,
            value = Some(new String(baos.toByteArray))
          )
          sendCommand(jsCmd)
        }
      }
    }

    /** Надо запросить токен у удаленного сервиса. */
    override def afterBecome(): Unit = {
      super.afterBecome()
      askToken {
        client.retrieveAccessToken(reqTok, verifier)
      }
    }

    /** Отработать результат запроса access_token'а. Т.е. выполнить завершение инициализации. */
    override def receiverPart: Receive = {
      case SuccessToken(acTok) =>
        trace("Have fresh access_token: " + acTok)
        // Зашифровать и сохранить токен в HTML5 localStorage:
        saveAcTokOnClient(acTok)
        become(new StartTargetActorsState(acTok))

      case Failure(ex) =>
        error("Failed to get new access token", ex)
        // Нарисовать юзеру на экране ошибку инициализации сервиса.
        renderInitFailed(ex)
        harakiri()
    }

  }


  /** Состояние запуска oauth1-target-акторов при наличии готового access-token'а. */
  class StartTargetActorsState(acTok: RequestToken) extends FsmState {
    /** При входе в состояние надо запустить всех акторов для всех имеющихся целей. */
    override def afterBecome(): Unit = {
      super.afterBecome()
      // Собрать target-акторов
      val tgActors = args.targets.map { tg =>
        trace("Creating oauth1-target-actor for tg = " + tg)
        val mctx3 = args.mctx0.copy(
          svcTargets = Nil,
          status = None,
          target = Some( tg2jsTg(tg) )
        )
        val actorArgs = new IOAuth1AdvTargetActorArgs with IExtAdvArgsWrapperT {
          override def mctx0              = mctx3
          override def target             = tg
          override def _eaArgsUnderlying  = args
          override def wsMediatorRef      = args.wsMediatorRef
          override def accessToken        = acTok
        }
        OAuth1TargetActor.props(actorArgs)
      }
      args.wsMediatorRef ! AddActors(tgActors)
      // Дело сделано, на этом наверное всё...
      harakiri()
    }

    override def receiverPart: Receive = PartialFunction.empty
  }

  /** Статически-типизированный контейнер токена-результата вместо Success(). */
  protected[this] case class SuccessToken(token: RequestToken)
}
