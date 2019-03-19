package controllers

import models.mctx.ContextT
import models.mproj.IMCommonDi
import models.req.MUserInits
import play.api.i18n.{I18nSupport, Lang, Messages}
import play.api.mvc._
import util.jsa.init.CtlJsInitT

import scala.concurrent.{ExecutionContext, Future}
import play.api.data.Form
import play.api.mvc.Result

import scala.language.implicitConversions
import io.suggest.flash.FlashConstants

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 11.10.13 11:43
 * Description: Базовый хелпер для контроллеров suggest.io. Используется почти всегда вместо обычного Controller.
 */
object SioController {

  def getRdrUrl(rdrPath: Option[String])(dflt: => Future[Call])(implicit ec: ExecutionContext): Future[String] = {
    rdrPath
      .filter(_ startsWith "/")
      .fold { dflt.map(_.url) }  { Future.successful }
  }

}

trait SioController
  extends InjectedController
  with ContextT
  with I18nSupport
  with CtlJsInitT
  with IMCommonDi
{

  import mCommonDi._

  implicit protected def simpleResult2async(sr: Result): Future[Result] = {
    Future.successful(sr)
  }

  def U = MUserInits

  /** Быстрый доступ к константам flash-статусов. */
  def FLASH = FlashConstants.Statuses

  /** Построчное красивое форматирование ошибок формы для вывода в логи/консоль. */
  def formatFormErrors(formWithErrors: Form[_]): String = {
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
    for {
      r <- SioController.getRdrUrl(rdrPath)(dflt)
    } yield {
      Redirect(r)
    }
  }


  // TODO Opt Надо, чтобы был Value class, но extends AnyVal нельзя делать внутри trait/class.
  implicit class ResultExtOps(val r: Result) {

    def cacheControl(seconds: Int): Result = {
      val v = if (isProd) {
        "public, max-age=" + seconds
      } else {
        "no-cache"
      }
      r.withHeaders(CACHE_CONTROL -> v)
    }

  }


  /** 2015.mar.30: Если в выбранном языке не указана страна, то нужно её туда прикрутить.
    * Появилось после добавления кодов стран к языкам messages. У части людей остались старые кукисы. */
  override implicit def request2Messages(implicit request: RequestHeader): Messages = {
    // TODO Следует брать дефолтовый Lang с учетом возможного ?lang=ru в qs запрашиваемой ссылки.
    // Тут должна быть проверка экземпляра реквеста http://www.mariussoutier.com/blog/2012/12/11/playframework-routes-part-2-advanced/
    // На уровне action builder'ов должна быть поддержка выставления языка из url qs.
    // Это решит все возможные проблемы с языками.
    /*val lang0 = request.getQueryString(LangUtil.LANG_QS_ARG_NAME)
      .flatMap { Lang.get }
      .getOrElse { super.request2lang }*/
    val messages0 = super.request2Messages
    val lang0 = messages0.lang
    if (lang0.country.nonEmpty) {
      messages0
    } else {
      // Нужно трансформировать язык к локаль исходя из доступных messages-локалей
      val avails = langs.availables
      val lang2 = avails
        .find { _.language == lang0.language }
        .orElse { Lang.get("en-US") }
        .orElse { avails.headOption }
        .getOrElse { Lang.defaultLang }
      messagesApi.preferred( lang2 :: lang0 :: Nil )
    }
  }

}

/** Абстрактная реализация контроллера с дедубликации скомпиленного кода между контроллерами. */
abstract class SioControllerImpl extends SioController

