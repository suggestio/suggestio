package controllers

import akka.actor.ActorSystem
import io.suggest.di.IExecutionContext
import play.api.i18n.{I18nSupport, Lang}
import play.api.mvc._
import util._
import util.di.ISioNotifier
import util.jsa.init.CtlJsInitT
import util.mail.IMailerWrapper
import util.ws.WsDispatcherActor
import scala.concurrent.Future
import scala.concurrent.duration._
import play.api.Play.{current, configuration}
import play.api.data.Form
import models._
import play.api.mvc.Result
import scala.language.implicitConversions
import io.suggest.flash.FlashConstants

import scala.util.{Failure, Success}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 11.10.13 11:43
 * Description: Базовый хелпер для контроллеров suggest.io. Используется почти всегда вместо обычного Controller.
 */
trait SioController
  extends Controller
  with ContextT
  with TplFormatUtilT
  with I18nSupport
  with CtlJsInitT
  with IExecutionContext
  with ISioNotifier     // TODO Спилить отсюда, не во всех контроллерах он нужен.
{

  implicit protected def simpleResult2async(sr: Result): Future[Result] = {
    Future.successful(sr)
  }

  /** Быстрый доступ к константам flash-статусов. */
  def FLASH = FlashConstants.Statuses

  /** Построчное красивое форматирование ошибок формы для вывода в логи/консоль. */
  def formatFormErrors(formWithErrors: Form[_]) = {
    formWithErrors.errors
      .iterator
      .map { e => "  " + e.key + " -> " + e.message }
      .mkString("\n")
  }
  

  // Обработка возвратов (?r=/../.../..) либо редиректов.
  /** Вернуть редирект через ?r=/... либо через указанный вызов. */
  def RdrBackOr(rdrPath: Option[String])(dflt: => Call): Result = {
    val rdrTo = rdrPath
      .filter(_ startsWith "/")
      .getOrElse(dflt.url)
    Redirect(rdrTo)
  }

  def RdrBackOrFut(rdrPath: Option[String])(dflt: => Future[Call]): Future[Result] = {
    rdrPath
      .filter(_ startsWith "/")
      .fold { dflt.map(_.url) }  { Future.successful }
      .map { r => Redirect(r) }
  }

  /** Бывает нужно просто впендюрить кеш для результата, но только когда продакшен. */
  def cacheControlShort(r: Result): Result = {
    val v = if (play.api.Play.isProd) {
      "public, max-age=600"
    } else {
      "no-cache"
    }
    r.withHeaders(CACHE_CONTROL -> v)
  }

  /** 2015.mar.30: Если в выбранном языке не указана страна, то нужно её туда прикрутить.
    * Появилось после добавления кодов стран к языкам messages. У части людей остались старые кукисы. */
   override implicit def request2lang(implicit request: RequestHeader): Lang = {
    // TODO Следует брать дефолтовый Lang с учетом возможного ?lang=ru в qs запрашиваемой ссылки.
    // Тут должна быть проверка экземпляра реквеста http://www.mariussoutier.com/blog/2012/12/11/playframework-routes-part-2-advanced/
    // На уровне action builder'ов должна быть поддержка выставления языка из url qs.
    // Это решит все возможные проблемы с языками.
    /*val lang0 = request.getQueryString(LangUtil.LANG_QS_ARG_NAME)
      .flatMap { Lang.get }
      .getOrElse { super.request2lang }*/
    val lang0 = super.request2lang
    if (!lang0.country.isEmpty) {
      lang0
    } else {
      // Нужно трансформировать язык к локаль исходя из доступных messages-локалей
      val avails = Lang.availables
      avails
        .find { _.language == lang0.language }
        .orElse { Lang.get("en-US") }
        .orElse { avails.headOption }
        .getOrElse { Lang.defaultLang }
    }
  }

}
/** Абстрактная реализация контроллера с дедубликации скомпиленного кода между контроллерами.
  * TODO Когда заработает proguard, этот класс надо будет выпилить начисто. */
abstract class SioControllerImpl extends SioController


/** Трейт, добавляющий константу, хранящую имя текущего модуля, пригодного для использования в конфиге в качестве ключа. */
trait MyConfName {
 
  /** Имя модуля в ключах конфига. Нельзя, чтобы ключ конфига содержал знак $, который скала добавляет
    * ко всем объектам. Используется только при инициализации. */
  val MY_CONF_NAME = getClass.getSimpleName.replace("$", "")
 
}


/** Утиль для связи с акторами, обрабатывающими ws-соединения. */
trait NotifyWs extends SioController with PlayMacroLogsI with MyConfName {

  /** akka system, приходящая в контроллер через DI. */
  def actorSystem: ActorSystem

  /** Сколько асинхронных попыток предпринимать. */
  val NOTIFY_WS_WAIT_RETRIES_MAX = configuration.getInt(s"ctl.ws.notify.$MY_CONF_NAME.retires.max") getOrElse NOTIFY_WS_WAIT_RETRIES_MAX_DFLT
  def NOTIFY_WS_WAIT_RETRIES_MAX_DFLT = 15

  /** Пауза между повторными попытками отправить уведомление. */
  val NOTIFY_WS_RETRY_PAUSE_MS = configuration.getLong(s"ctl.ws.notify.$MY_CONF_NAME.retry.pause.ms") getOrElse NOTIFY_WS_RETRY_PAUSE_MS_DFLT
  def NOTIFY_WS_RETRY_PAUSE_MS_DFLT = 1000L

  /** Послать сообщение ws-актору с указанным wsId. Если WS-актор ещё не появился, то нужно подождать его
    * некоторое время. Если WS-актор так и не появился, то выразить соболезнования в логи. */
  def _notifyWs(wsId: String, msg: Any, counter: Int = 0): Unit = {
    WsDispatcherActor.getForWsId(wsId)
      .onComplete {
        case Success(Some(wsActorRef)) =>
          wsActorRef ! msg
        case other =>
          if (counter < NOTIFY_WS_WAIT_RETRIES_MAX) {
            actorSystem.scheduler.scheduleOnce(NOTIFY_WS_RETRY_PAUSE_MS.milliseconds) {
              _notifyWs(wsId, msg, counter + 1)
            }
            other match {
              //case Success(None) =>
              //  LOGGER.trace(s"WS actor $wsId not exists right now. Will retry after $NOTIFY_WS_RETRY_PAUSE_MS ms...")
              case Failure(ex) =>
                LOGGER.warn(s"Failed to ask ws-actor-dispatcher about WS actor [$wsId]", ex)
              // подавляем warning на Success(Some(_)), который отрабатывается выше
              case _ =>
                // should never happen
            }
          } else {
            LOGGER.debug(s"WS message to $wsId was not sent and dropped, because actor not found: $msg , Last error was: $other")
          }
      }
  }

}


/** Интерфейс для mailer'а.  */
trait IMailer {
  def mailer: IMailerWrapper
}
