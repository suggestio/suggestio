package controllers

import models.mctx.ContextT
import models.mproj.{ICommonDi, IMCommonDi}
import models.req.MUserInits
import play.api.i18n.{I18nSupport, Lang, Messages}
import play.api.mvc._
import util.jsa.init.CtlJsInitT

import scala.concurrent.{ExecutionContext, Future}
import play.api.data.Form
import play.api.mvc.Result

import scala.language.implicitConversions
import io.suggest.flash.FlashConstants
import io.suggest.util.logs.MacroLogsImpl
import javax.inject.{Inject, Singleton}
import japgolly.univeq._

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
      .fold { dflt.map(_.url) } { Future.successful }
  }

}


/** Статическое расшаренное API для HTTP-контроллера play.
  *
  * Стандартные контроллеры хранят кучу инстансов Status и прочего,
  * якобы для какого-то повышения производительности в синтетических тестах.
  */
@Singleton
final class SioControllerApi @Inject()(
                                        override val mCommonDi: ICommonDi
                                      )
  extends InjectedController
  with ContextT
  with I18nSupport
  with CtlJsInitT
  with IMCommonDi
  with MacroLogsImpl
{

  import mCommonDi._

  implicit def simpleResult2async(sr: Result): Future[Result] = {
    Future.successful(sr)
  }

  val U = MUserInits

  /** Быстрый доступ к константам flash-статусов. */
  val FLASH = FlashConstants.Statuses

  /** Построчное красивое форматирование ошибок формы для вывода в логи/консоль. */
  def formatFormErrors(formWithErrors: Form[_]): String = {
    formWithErrors.errors
      .iterator
      .map { e =>
        s"  ${e.key} -> ${e.message}"
      }
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


  /** Доп.API для Result'ов. */
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
      LOGGER.trace(s"request2messages: lang0=${lang0.code} <= cookie#${request.cookies.get(messagesApi.langCookieName).getOrElse("")} accepted=${request.acceptLanguages.iterator.map(_.code).mkString("|")} avails=${langs.availables.iterator.map(_.code).mkString(",")} ${request.transientLang().fold("")(" transient=" + _)}")
      messages0
    } else {
      // Нужно трансформировать язык к локаль исходя из доступных messages-локалей
      val avails = langs.availables
      val lang2 = avails
        .find { _.language ==* lang0.language }
        .orElse {
          LOGGER.trace(s"request2messages: lang0=${lang0.code} avails=[${avails.iterator.map(_.code).mkString("|")}] - nothing matched")
          Lang.get("en")
        }
        .orElse {
          LOGGER.debug(s"request2messages: en-US not found in lang.avails=[${avails.iterator.map(_.code).mkString("|")}], lang0=${lang0.code}")
          avails.headOption
        }
        .getOrElse {
          val r = Lang.defaultLang
          LOGGER.warn(s"request2messages: lang.avails is empty! default-lang=$r  lang0=${lang0.code}")
          r
        }

      messagesApi.preferred( lang2 :: lang0 :: Nil )
    }
  }

}


trait ISioControllerApi {
  val sioControllerApi: SioControllerApi
}
