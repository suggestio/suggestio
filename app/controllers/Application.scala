package controllers

import play.api.mvc._
import util.ContextImpl
import util.acl._
import views.html.crawl._
import play.api.i18n.Lang
import io.suggest.util.LogsImpl
import play.api.Play.current
import scala.concurrent.Future
import models.MPerson
import play.api.libs.concurrent.Execution.Implicits._

object Application extends SioController {

  private val LOGGER = new LogsImpl(getClass)
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
  def change_locale(locale: String) = MaybeAuth { implicit request =>
    val referrer = request.headers.get(REFERER).getOrElse("/")
    
    debug("Change user lang to : " + locale)
    val lang1 = Lang(locale)

    // Нужно сохранять смену языка в БД, если юзер залогинен.
    if (request.isAuth) {
      val pw = request.pwOpt.get
      val langCode = lang1.language
      pw.personOptFut.flatMap { result =>
        val result1 = result match {
          case Some(person) =>
            person.lang = langCode
            person

          case None => MPerson(pw.id, lang = langCode)
        }
        result1.save

      } onFailure {
        case ex => warn("Failed to save lang settings for user " + pw.id, ex)
      }
    }

    Redirect(referrer).withLang(lang1)
    // TODO Check if the lang is handled by the application
  }


  /** Тело экшена, генерирующее страницу 404. Используется при минимальном окружении. */
  def http404(implicit request: RequestHeader): SimpleResult = {
    implicit val ctx = ContextImpl()
    NotFound(views.html.static.http404Tpl())
  }

  /** Враппер, генерящий фьючерс с телом экшена http404(RequestHeader). */
  def http404Fut(implicit request: RequestHeader): Future[SimpleResult] = http404

}
