package controllers.cstatic

import controllers.SioController
import models.Context
import play.api.Play
import play.api.libs.iteratee.Enumerator
import util.acl.IgnoreAuth
import util.seo.SiteMapUtil
import views.html.static.sitemap._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 17.12.15 11:45
 * Description: Трейт для контроллера для поддержки экшена с раздачей sitemap'ов.
 */
trait SiteMapsXml extends SioController {

  def siteMapUtil: SiteMapUtil

  import mCommonDi._

  /** Время кеширования /sitemap.xml ответа на клиенте. */
  private val SITEMAP_XML_CACHE_TTL_SECONDS: Int = {
    configuration.getInt("sitemap.xml.cache.ttl.seconds") getOrElse {
      if (Play.isDev) 1 else 60
    }
  }


  /**
   * Раздача сайт-мапы.
   * @return sitemap, генерируемый поточно с очень минимальным потреблением RAM.
   */
  def siteMapXml = IgnoreAuth { implicit request =>
    implicit val ctx = implicitly[Context]
    val enums = siteMapUtil.SITEMAP_SOURCES
      .map(_.siteMapXmlEnumerator(ctx))
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

}
