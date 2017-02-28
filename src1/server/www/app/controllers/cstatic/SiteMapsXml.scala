package controllers.cstatic

import controllers.SioController
import models.mctx.Context
import play.api.libs.iteratee.Enumerator
import util.acl.IIgnoreAuth
import util.seo.SiteMapUtil
import views.html.static.sitemap._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 17.12.15 11:45
 * Description: Трейт для контроллера для поддержки экшена с раздачей sitemap'ов.
 */
trait SiteMapsXml extends SioController with IIgnoreAuth {

  import mCommonDi._


  def siteMapUtil: SiteMapUtil

  /** Время кеширования /sitemap.xml ответа на клиенте. */
  private val SITEMAP_XML_CACHE_TTL_SECONDS: Int = {
    configuration.getInt("sitemap.xml.cache.ttl.seconds") getOrElse {
      if (isDev) 1 else 60
    }
  }


  /**
   * Раздача сайт-мапы.
   * @return sitemap, генерируемый поточно с очень минимальным потреблением RAM.
   */
  def siteMapXml = ignoreAuth() { implicit request =>
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
      .as("text/xml")
      .withHeaders(
        CACHE_CONTROL -> s"public, max-age=$SITEMAP_XML_CACHE_TTL_SECONDS"
      )
  }

}
