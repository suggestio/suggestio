package controllers

import com.google.inject.Inject
import play.api.libs.iteratee.Enumerator
import play.api.mvc._
import util.acl._
import util.cdn.CorsUtil
import play.api.i18n.MessagesApi
import play.api.Play, Play.{current, configuration}
import util.seo.SiteMapUtil
import play.api.libs.concurrent.Execution.Implicits._
import views.html.static.sitemap._
import views.html.sys1._

class Application @Inject() (val messagesApi: MessagesApi) extends SioControllerImpl {

  /** Время кеширования /robots.txt ответа на клиенте. */
  private val ROBOTS_TXT_CACHE_TTL_SECONDS = configuration.getInt("robots.txt.cache.ttl.seconds") getOrElse {
    if (Play.isDev) 5 else 120
  }

  /** Время кеширования /sitemap.xml ответа на клиенте. */
  private val SITEMAP_XML_CACHE_TTL_SECONDS = configuration.getInt("sitemap.xml.cache.ttl.seconds") getOrElse {
    if (Play.isDev) 1 else 60
  }


  /** Раздача содержимого robots.txt. */
  def robotsTxt = Action { implicit request =>
    Ok(views.txt.static.robotsTxtTpl())
      .withHeaders(
        CONTENT_TYPE  -> "text/plain; charset=utf-8",
        CACHE_CONTROL -> s"public, max-age=$ROBOTS_TXT_CACHE_TTL_SECONDS"
      )
  }

  /**
   * Экшен для скрытого продления сессии в фоне. Может дергаться в js'ом незаметно.
   * @return 204 No Content - всё ок.
   *         Другой код - сессия истекла.
   */
  def keepAliveSession = IsAuth { implicit request =>
    NoContent
  }


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


  /**
   * Раздача сайт-мапы.
   * @return sitemap, генерируемый поточно с очень минимальным потреблением RAM.
   */
  def siteMapXml = MaybeAuth { implicit request =>
    implicit val ctx = getContext2
    val enums = SiteMapUtil.SITEMAP_SOURCES.map(_.siteMapXmlEnumerator(ctx))
    val urls = Enumerator.interleave(enums)
      .map { _urlTpl(_) }
    // Нужно добавить к сайтмапу начало и конец xml. Дорисовываем enumerator'ы:
    val respBody = Enumerator( beforeUrlsTpl()(ctx) )
      .andThen(urls)
      .andThen( Enumerator(1) map {_ => afterUrlsTpl()(ctx)} )  // Форсируем отложенный рендер футера через map()
      .andThen( Enumerator.eof )
    Ok.feed(respBody)
      .withHeaders(
        CONTENT_TYPE  -> "text/xml",
        CACHE_CONTROL -> s"public, max-age=$SITEMAP_XML_CACHE_TTL_SECONDS"
      )
  }


  // 2014.jan.18: Из ctl.Sys удалена поддержка siobix в связи с заброшенностью и
  // экстренным выпиливанием лишних зависимостей.
  // Последний коммит-родитель: 5572decfec00
  // В sys-контроллере остался только один экшен, поэтому он был закинут в эту помойку.

  /** indexTpl.scala.html для системной панели. */
  def sysIndex = IsSuperuserOr404 { implicit request =>
    Ok(indexTpl())
  }

}

