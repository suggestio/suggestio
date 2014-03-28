package controllers

import play.api.mvc.{RequestHeader, SimpleResult, Controller}
import util.{ContextImpl, HtmlCompressUtil, ContextT}
import scala.concurrent.{Promise, Future}
import play.api.i18n.Lang
import util.event.SiowebNotifier
import play.api.templates.{TxtFormat, HtmlFormat}
import play.api.libs.json.JsString
import play.api.libs.concurrent.Akka
import scala.concurrent.duration._
import play.api.Play.current
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.data.Form

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 11.10.13 11:43
 * Description: Базис для контроллеров s.io.
 */

trait SioController extends Controller with ContextT {

  implicit protected def simpleResult2async(sr: SimpleResult): Future[SimpleResult] = {
    Future.successful(sr)
  }

  implicit def sn = SiowebNotifier

  implicit def html4email(html: HtmlFormat.Appendable) = HtmlCompressUtil.compressForEmail(html)

  implicit def html2jsStr(html: HtmlFormat.Appendable) = JsString(
    HtmlCompressUtil.compressForJson(html)
  )

  implicit def txt2str(txt: TxtFormat.Appendable): String = txt.body.trim

  implicit def txt2jsStr(txt: TxtFormat.Appendable) = JsString(txt)

  /** Построчное красивое форматирование ошибок формы для вывода в логи/консоль. */
  def formatFormErrors(formWithErrors: Form[_]) = {
    formWithErrors.errors.map { e => "  " + e.key + " -> " + e.message }.mkString("\n")
  }
  
  /** Тело экшена, генерирующее страницу 404. Используется при минимальном окружении. */
  def http404AdHoc(implicit request: RequestHeader): SimpleResult = {
    implicit val ctx = ContextImpl()
    NotFound(views.html.static.http404Tpl())
  }

}


trait BruteForceProtect {

  val INVITE_CHECK_LAG_DURATION = 333 millis

  /** Система асинхронного платформонезависимого противодействия брутфорс-атакам. */
  // TODO Надо вынести её код в util.
  def bruteForceProtect: Future[_] = {
    // Для противодействию брутфорсу добавляем асинхронную задержку выполнения проверки по методике https://stackoverflow.com/a/17284760
    // TODO Нужно лимитировать попытки по IP клиента. ip можно закидывать в cache с коротким ttl.
    val lagPromise = Promise[Unit]()
    Akka.system.scheduler.scheduleOnce(INVITE_CHECK_LAG_DURATION) {
      lagPromise.success()
    }
    lagPromise.future
  }

}

