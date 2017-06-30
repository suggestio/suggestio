package util.adv.ext

import java.io.ByteArrayOutputStream
import java.time.OffsetDateTime
import java.util.concurrent.TimeoutException

import akka.actor.Props
import com.google.inject.assistedinject.Assisted
import javax.inject.{Inject, Singleton}

import controllers.routes
import io.suggest.async.AsyncUtil
import io.suggest.fsm.FsmActor
import io.suggest.primo.IToPublicString
import io.suggest.sec.m.{MAsymKey, MAsymKeys}
import io.suggest.sec.util.PgpUtil
import io.suggest.util.logs.MacroLogsImpl
import models.adv._
import models.adv.ext.act.{ActorPathQs, ExtServiceActorEnv, OAuthVerifier}
import models.adv.js._
import models.adv.js.ctx.MStorageKvCtx
import models.event.ErrorInfo
import models.jsm.DomWindowSpecs
import models.ls.LsOAuth1Info
import models.mctx.ContextUtil
import models.mproj.ICommonDi
import org.apache.commons.io.IOUtils
import play.api.libs.json.Json
import play.api.libs.oauth.RequestToken
import play.api.libs.ws.WSClient
import play.shaded.oauth.oauth.signpost.exception.OAuthException
import util.adv.ext.ut._
import util.ext.ExtServicesUtil
import util.jsa.JsWindowOpen

import scala.concurrent.Future
import scala.concurrent.duration._
import scala.util.{Failure, Success}

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 03.04.15 21:23
  * Description: Файл содержит подчиненный WebSocket-актор и
  * смежную утиль для подготовки работы сервиса OAuth1.
  */

/** Guice-factory для сборки инстансов акторов. */
trait OAuth1ServiceActorFactory
  extends IApplyServiceActor[OAuth1ServiceActor]


/**
  * service-level actor для подготовки OAuth1 к работе в рамках одного oauth1-сервиса.
  * Актор занимается инициализацией состояния OAuth1-контекста, а именно получением access_token'а.
  * TODO Ранее полученный токен хранится в куках у клиента, но нужно производить проверку его.
  *
  * @param args Аргументы для актора.
  * @see [[https://www.playframework.com/documentation/2.4.x/ScalaOAuth]]
  */
class OAuth1ServiceActor @Inject() (
                                     @Assisted override val args : IExtAdvServiceActorArgs,
                                     oa1TgActorFactory           : OAuth1TargetActorFactory,
                                     mCommonDi                   : ICommonDi,
                                     oa1SvcActorUtil             : OAuth1SvcActorUtil,
                                     pgpUtil                     : PgpUtil,
                                     mAsymKeys                   : MAsymKeys,
                                     asyncUtil                   : AsyncUtil,
                                     override val extServicesUtil: ExtServicesUtil,
                                     override val ctxUtil        : ContextUtil,
                                     override val advExtFormUtil : AdvExtFormUtil,
                                     implicit val wsClient       : WSClient
                                   )
  extends FsmActor
  with ReplyTo
  with ExtServiceActorEnv
  with MediatorSendCommand
  with MacroLogsImpl
  with SvcActorJsRenderUtil
{

  import LOGGER._
  import mCommonDi.ec
  import oa1SvcActorUtil._

  override type State_t = FsmState

  override protected var _state: FsmState = new DummyState

  override def receive: Receive = allStatesReceiver

  private val oa1Support = serviceHelper.oauth1Support.get

  /** OAuth1-клиент сервиса. */
  private val oa1client = oa1Support.client

  /** Ключ шифрования-дешифрования для хранения данных в localStorage. */
  lazy val lsCryptoKey: Future[MAsymKey] = {
    val keyId = pgpUtil.LOCAL_STOR_KEY_ID
    val fut = mAsymKeys.getById(keyId)
      .map(_.get)
    fut.onFailure {
      case _: NoSuchElementException =>
        warn("Server normal PGP key not yet created! I cannot store access tokens!")
      case ex: Throwable =>
        error("Cannot read server PGP key: " + keyId, ex)
    }
    fut
  }

  /** Ключ для хранения секретов access_token'а, относящихся к юзеру. */
  lazy val lsValueKey = s"adv.ext.svc.${args.service.strId}.access.${args.request.user.personIdOpt.getOrElse("__ANON__")}"

  /** Имя js-попапа, в рамках которого происходит авторизация пользователя сервисом. */
  def domWndTargetName = "popup-authz-" + args.service.strId

  /** Запуск актора. Выставить исходное состояние. */
  override def preStart(): Unit = {
    super.preStart()
    // Сразу начинаем выискивать ранее сохраненный access_token.
    become(new GetSavedAcTokFromUserState)
  }

  /** Псевдоасинхронный запрос какого-то токена у удаленного сервиса. */
  protected def askToken(f: => Either[OAuthException, RequestToken]): Unit = {
    Future(f)(asyncUtil.singleThreadIoContext)
      // Завернуть результат в правильный Future.
      .flatMap {
        case Right(reqTok)  => Future.successful( reqTok )
        case Left(ex)       => Future.failed( ex )
      }
      // Сообщить текущему актору о результатах.
      .onComplete {
        case Success(res)   => self ! SuccessToken(res)
        case failure        => self ! failure
      }
  }

  /**
   * Рендер ошибки, возникшей в ходе инициализации сервиса.
   *
   * @param ex Исключение. Как правило, это экземпляр OAuthException.
   */
  protected def renderInitFailed(ex: Throwable): Unit = {

    serviceInitFailedRender(
      errors = Seq(ErrorInfo(
        msg = "e.adv.ext.api.init",
        info = Some(s"${ex.getClass.getSimpleName}: ${IToPublicString.getPublicString(ex)}")
      ))
    )
  }

  /** Отправить команду записи в хранилище браузера. */
  protected def sendStorageSetCmd(value2: Option[String]): Unit = {
    val cctx = MStorageKvCtx(
      key = lsValueKey,
      value = value2
    )
    val mctx1 = args.mctx0.copy(
      action = Some( MJsActions.StorageSet ),
      custom = Some( Json.toJson(cctx) )
    )
    val jsCmd = StorageSetCmd(mctx1)
    sendCommand(jsCmd)
  }

  /** Сериализовать, зашифровать, подписать и сохранить в DOM.localStorage экземпляр LS-модели токенов. */
  protected def saveOa1Info(info: LsOAuth1Info): Future[_] = {
    val pgpKeyFut = lsCryptoKey
    val json = Json.toJson(info).toString()
    // Зашифровать всё с помощью PGP.
    val baos = new ByteArrayOutputStream(1024)
    pgpKeyFut map { pgpKey =>
      pgpUtil.encryptForSelf(
        data = IOUtils.toInputStream(json),
        key  = pgpKey,
        out  = baos
      )
      sendStorageSetCmd( Some(new String(baos.toByteArray)) )
    }
  }


  /** Запросить ранее сохраненный access_token из браузера клиента. */
  class GetSavedAcTokFromUserState extends FsmState {

    protected var answerReceived: Boolean = false

    protected lazy val timeoutTimer = {
      context.system.scheduler.scheduleOnce( LS_STORED_TOKEN_ASK_TIMEOUT_SEC.seconds ) {
        if (!answerReceived) {
          self ! new TimeoutException("localStorage.getItem ask timeout")
          warn("Timeout while waiting for stored access_token result from user.")
        }
      }
    }

    override def afterBecome(): Unit = {
      super.afterBecome()
      val cctx = MStorageKvCtx(lsValueKey)
      val mctx1 = args.mctx0.copy(
        action = Some( MJsActions.StorageGet ),
        custom = Some( Json.toJson(cctx) )
      )
      val jsCmd = StorageGetCmd(
        mctx    = mctx1,
        replyTo = Some(replyTo)
      )
      sendCommand(jsCmd)
      // Нужен timeout на случай проблем. Запустить его сейчас.
      timeoutTimer
      // Прогревка: запустить получение ключа дешифровки из модели ключей. Он понадобится в последующих состояниях.
      lsCryptoKey
    }

    override def receiverPart: Receive = {
      // Пришел ответ от js с результатами чтения хранилища браузера. Нужно попытаться расшифровать его.
      case ans: Answer if ans.ctx2.action.contains(MJsActions.StorageGet) =>
        // Остановить таймер таймаута.
        answerReceived = true
        timeoutTimer.cancel()
        become(new DecryptStoredAccessTokenState(ans))

      // Таймаут наступил. Значит запускаем процесс получения токена с юзера.
      case te: TimeoutException =>
        if (!answerReceived)
          become( new AskRequestTokenState(false) )
    }

  }


  /** Получить ключ из хранилища и дождаться расшифровки данных. */
  class DecryptStoredAccessTokenState(ans: Answer) extends FsmState {
    /** Действия, которые вызываются, когда это состояние выставлено в актор. */
    override def afterBecome(): Unit = {
      super.afterBecome()
      // Извлекаем возможное значение.
      val cctxOpt = ans.ctx2.custom
        .flatMap { jsv  =>  Json.fromJson[MStorageKvCtx](jsv).asOpt }
        .filter  { _.key == lsValueKey }
        .flatMap { _.value }
      val fut = cctxOpt match {
        case Some(input) =>
          for {
            mkey  <- lsCryptoKey
          } yield {
            val input = cctxOpt.get
            val baos = new ByteArrayOutputStream(256)
            pgpUtil.decryptFromSelf(
              data = IOUtils.toInputStream( input ),
              key  = mkey,
              out  = baos
            )
            Json.parse( baos.toByteArray )
              .asOpt[LsOAuth1Info]
              // Убедится, что токен был выдан именно текущему юзеру.
              .filter { info =>
                val currPersonIdOpt = args.request.user.personIdOpt
                val res = currPersonIdOpt.contains( info.personId )
                if (!res)
                  warn(s"[XAKEP] User $currPersonIdOpt is detected while tried to use foreign access token: orig ownerId = ${info.personId} since ${info.created}")
                res
              }
              // Если нет значения или оно чужое, то будет экзепшен.
              .get
          }

        case None =>
          val ex = new NoSuchElementException("cctx contains no or unrelated value")
          Future.failed( ex )
      }
      fut onComplete {
        // Успешно получен ранее сохраненный токен от клиента.
        case Success(oaInfo) =>
          self ! oaInfo
        case Failure(ex) =>
          self ! FailedInfo(ex, cctxOpt.isDefined)
          if ( !ex.isInstanceOf[NoSuchElementException] )
            warn("Failed to restore access token from user storage, answer = " + ans, ex)
          else
            debug("No saved value found: " + ex.getMessage)
      }
    }


    override def receiverPart: Receive = {
      // Получен ранее сохраненный access_token, но пока точно неизвестно, валиден ли этот токен сейчас.
      case oaInfo: LsOAuth1Info =>
        // TODO Не проверять токен слишком часто. Использовать таймштамы для кеширования валидности.
        trace("Have previosly stored access token since " + oaInfo.created)
        val nextState = if ( isStoredTokenNeedReverify(oaInfo) ) {
          new VerifyStoredAccessToken(oaInfo)
        } else {
          new StartTargetActorsState(oaInfo.acTok)
        }
        become(nextState)

      // Не удалось восстановить ранее сохраненный токен.
      case fi: FailedInfo =>
        become( new AskRequestTokenState(fi.userHaveInvalidTok) )
    }

    /** Контейнер ошибочного результата. */
    protected case class FailedInfo(ex: Throwable, userHaveInvalidTok: Boolean)
  }


  /** Состоянии верификации access_token'а силами удалённого сервиса. */
  class VerifyStoredAccessToken(oa1Info: LsOAuth1Info) extends FsmState {
    // Нужно запустить обращение к системе верификации access-токена.
    override def afterBecome(): Unit = {
      super.afterBecome()
      oa1Support.isAcTokValid(oa1Info.acTok) onComplete {
        case Success(isValid) => self ! isValid
        case other            => self ! other
      }
    }

    /** Результат работы http-клиета форвардится сюда. */
    override def receiverPart: Receive = {
      // Верификация прошла успешно. Перезаписать на клиенте проверенный access_token с новым временем последней проверки.
      case true =>
        val info1 = oa1Info.copy(verified = Some(OffsetDateTime.now))
        saveOa1Info(info1)
        trace("Access token verified successfully.")
        become( new StartTargetActorsState(oa1Info.acTok) )
      // Сервер сказал, что токен не валиден.
      case false =>
        // TODO При наступлении rate_limit, надо наверное допускать access_token.
        debug("Failed to verify previously saved access token. Let's request new one.")
        become( new AskRequestTokenState(userHaveInvalTok = true) )
      // Произошла какая-то ошибка.
      case Failure(ex) =>
        warn("Failed to make ac-tok verify HTTP request.", ex)
        become(new AskRequestTokenState(userHaveInvalTok = true))
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
          adnId       = args.request.producer.id.get,
          actorInfoQs = ActorPathQs(self.path)
        )
        // Вычисляем URL prefix. в devel-режиме нужно использовать ip локалхоста, а не его имя.
        val urlPrefix = ctxUtil.devReplaceLocalHostW127001( ctxUtil.LK_URL_PREFIX )
        // Заставить клиента открыть всплывающее окно для авторизации на твиттере.
        val returnUrl = urlPrefix + returnCall.url
        oa1client.retrieveRequestToken(returnUrl)
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
        // Отрендерить ошибку инициализации сервиса по ws.
        renderInitFailed(ex)
        // Пока возможностей типа "попробовать снова" нет, поэтому сразу завершаемся.
        harakiri()
    }
  }


  /**
   * Состояние отправки юзера на авторизацию в сервис и возврата назад.
   *
   * @param reqTok Полученный токен.
   */
  class RemoteUserAuthorizeState(reqTok: RequestToken, userHaveInvalTok: Boolean) extends FsmState {
    /** Действия, которые вызываются, когда это состояние выставлено в актор. */
    override def afterBecome(): Unit = {
      super.afterBecome()
      // Нужно отправить команду для отображения попапа с логином в твиттер (попап в порядке очереди).
      val wndSz = oa1Support.popupWndSz
      val someFalse = Some(false)
      val jsa = JsWindowOpen(
        url = oa1client.redirectUrl(reqTok.token),
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
        // Если у юзера был сохраненный в браузере токен, то стереть его.
        if (userHaveInvalTok) {
          sendStorageSetCmd(None)
        }
        // TODO Вывалить ошибку инициализации юзеру
    }
  }


  /** Состояние получения access_token'а. */
  class RetrieveAccessTokenState(reqTok: RequestToken, verifier: String) extends FsmState {

    /** Сохранение access_token'а на клиенте. */
    def saveAcTokOnClient(acTok: RequestToken): Unit = {
      args.request.user.personIdOpt match {
        case None =>
          debug("NOT saving access_token, because user is anonymous.")
        case Some(personId) =>
          val info = LsOAuth1Info(acTok, personId)
          saveOa1Info(info)
      }
    }

    /** Надо запросить токен у удаленного сервиса. */
    override def afterBecome(): Unit = {
      super.afterBecome()
      askToken {
        oa1client.retrieveAccessToken(reqTok, verifier)
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
          svcTargets  = Nil,
          status      = None,
          target      = Some( tg2jsTg(tg) )
        )
        // Сборка финального актора и его аргументов.
        val actorArgs = MOAuth1AdvTargetActorArgs(
          mctx0               = mctx3,
          target              = tg,
          _eaArgsUnderlying   = args,
          wsMediatorRef       = args.wsMediatorRef,
          accessToken         = acTok
        )
        Props( oa1TgActorFactory(actorArgs) )
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


/** Всякая shared-утиль для акторов [[OAuth1ServiceActor]] сбрасывается сюда. */
@Singleton
class OAuth1SvcActorUtil {

  /** Таймаут спрашивания у юзера ранее сохраненных данных по access-token'у. */
  def LS_STORED_TOKEN_ASK_TIMEOUT_SEC = 3

  /** Время жизни токена, после которого надо сделать верификацию. */
  def VERIFY_TTL_SECONDS = 600

  /** Наступила ли пора для ревалидации токена? */
  def isStoredTokenNeedReverify(lsOAuth1Info: LsOAuth1Info): Boolean = {
    val lastVerified = lsOAuth1Info.verified
      .getOrElse( lsOAuth1Info.created )
    OffsetDateTime.now()
      .minusSeconds(VERIFY_TTL_SECONDS)
      .isAfter( lastVerified )
  }

}

