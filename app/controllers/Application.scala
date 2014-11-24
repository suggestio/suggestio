package controllers

import models.Context
import models.crawl.SiteMapUrlT
import play.api.libs.iteratee.Enumerator
import play.api.mvc._
import play.twirl.api.Html
import util.PlayMacroLogsImpl
import util.acl._
import util.cdn.CorsUtil
import play.api.i18n.Lang
import play.api.Play.{current, configuration}
import scala.concurrent.Future
import play.api.libs.concurrent.Execution.Implicits._
import util.SiowebEsUtil.client
import views.html.static.sitemap._

object Application extends SioController with PlayMacroLogsImpl {

  import LOGGER._

  /** Раздача содержимого robots.txt. */
  def robotsTxt = Action { implicit request =>
    Ok(views.txt.static.robotsTxtTpl())
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


  /** Источники для наполнения sitemap.xml */
  private def SITEMAP_SOURCES: Seq[SiteMapXmlCtl] = Seq(MarketShowcase, Market)

  /**
   * Раздача сайт-мапы.
   * @return sitemap, генерируемый поточно с очень минимальным потреблением RAM.
   */
  def siteMapXml = MaybeAuth { implicit request =>
    implicit val ctx = getContext2
    val enums = SITEMAP_SOURCES.map(_.siteMapXmlEnumerator(ctx))
    val urls = Enumerator.interleave(enums)
      .map { _urlTpl(_) }
    // Нужно добавить к сайтмапу начало и конец xml. Дорисовываем enumerator'ы:
    val sxPrefix = Enumerator( beforeUrlsTpl()(ctx) )
    val respBody = sxPrefix
      .andThen(urls)
      .andThen( Enumerator(1) map {_ => afterUrlsTpl()(ctx)} )  // Форсируем асинхронный рендер через map()
      .andThen( Enumerator.eof )
    Ok.feed(respBody)
      .as("text/xml")
  }

}

/** Интерфейс для контроллеров, которые раздают страницы, подлежащие публикации в sitemap.xml. */
trait SiteMapXmlCtl {
  /** Асинхронно поточно генерировать данные о страницах, подлежащих индексации. */
  def siteMapXmlEnumerator(implicit ctx: Context): Enumerator[SiteMapUrlT]
}
