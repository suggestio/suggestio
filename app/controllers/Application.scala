package controllers

import play.api.mvc._
import util.PlayMacroLogsImpl
import util.acl._
import util.cdn.CorsUtil
import views.html.crawl._
import views.html.stuff._
import play.api.i18n.Lang
import play.api.Play.{current, configuration}
import scala.concurrent.Future
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

  def xd_server  = MaybeAuth { implicit request =>
    Ok(xdServerTpl())
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
          val person2 = person.copy(
            lang = langCode
          )
          person2.save
            .map {_ => resp0 }

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

  /**
   * Экшен для скрытого продления сессии в фоне. Может дергаться в js'ом незаметно.
   * @return 204 No Content - всё ок.
   *         Другой код - сессия истекла.
   */
  def keepAliveSession = IsAuth { implicit request =>
    NoContent
  }

  /** Враппер, генерящий фьючерс с телом экшена http404(RequestHeader). */
  def http404Fut(implicit request: RequestHeader): Future[Result] = http404AdHoc

  /**
   * Реакция на options-запрос, хидеры выставит CORS-фильтр, подключенный в Global.
   * @param path Путь, к которому запрошены опшыны.
   * @return
   */
  def corsPreflight(path: String) = Action {
    if (CorsUtil.CORS_PREFLIGHT_ALLOWED)
      Ok
    else
      NotFound
  }

}
