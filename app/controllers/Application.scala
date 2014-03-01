package controllers

import play.api.mvc._
import util.{PlayMacroLogsImpl, ContextImpl}
import util.acl._
import views.html.crawl._
import play.api.i18n.Lang
import play.api.Play.current
import scala.concurrent.Future
import models.MPerson
import play.api.libs.concurrent.Execution.Implicits._
import util.SiowebEsUtil.client

object Application extends SioController with PlayMacroLogsImpl {

  import LOGGER._

  /**
   * Выдача главной страницы
   */
  def index = MaybeAuth { implicit request =>
    Ok(indexTpl())
  }

  /** Форма быстрого поиска на произвольном сайте. Используется в служебных целях в основном, в /sys/. */
  def search = MaybeAuth { implicit request =>
    Ok(searchTpl())
  }

  /** Запрос смены языка UI. */
  def change_locale(locale: String) = MaybeAuth.async { implicit request =>
    val referrer = request.headers.get(REFERER).getOrElse("/")
    trace("Change user lang to : " + locale)
    // TODO Проверять язык по списку доступных.
    val lang1 = Lang(locale)
    val resp0 = Redirect(referrer).withLang(lang1)
    // Нужно сохранять смену языка в БД, если юзер залогинен.
    if (request.isAuth) {
      val pw = request.pwOpt.get
      val langCode = lang1.language
      pw.personOptFut.flatMap {
        case Some(person) =>
          person.lang = langCode
          person.save.map {_ => resp0 }

        case None =>
          // TODO Внезапно неизвесный юзер с правильными кукисами в сессии.
          // Нужно сбросить ему сессию и выставить куку с иной локалью.
          warn(s"User with valid session personId=${pw.personId} not found in DB! Resetting session...")
          resp0.withNewSession
      }
    } else {
      resp0
    }
  }


  /** Тело экшена, генерирующее страницу 404. Используется при минимальном окружении. */
  def http404(implicit request: RequestHeader): SimpleResult = {
    implicit val ctx = ContextImpl()
    NotFound(views.html.static.http404Tpl())
  }

  /** Враппер, генерящий фьючерс с телом экшена http404(RequestHeader). */
  def http404Fut(implicit request: RequestHeader): Future[SimpleResult] = http404

}
