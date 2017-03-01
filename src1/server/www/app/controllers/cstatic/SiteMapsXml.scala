package controllers.cstatic

import akka.stream.scaladsl.Source
import controllers.SioController
import models.mctx.Context
import play.api.http.{HttpEntity, Writeable}
import play.twirl.api.Xml
import util.acl.IIgnoreAuth
import util.seo.SiteMapUtil
import views.xml.static.sitemap._

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
  private def SITEMAP_XML_CACHE_TTL_SECONDS = if (isDev) 1 else 120


  /**
   * Раздача сайт-мапы.
   * @return sitemap, генерируемый поточно с очень минимальным потреблением RAM.
   */
  def siteMapXml = ignoreAuth() { implicit request =>
    implicit val ctx = implicitly[Context]

    val srcDescrs = siteMapUtil.SITEMAP_SOURCES

    // Собираем асинхронный неупорядоченный источник sitemap-ссылок:
    val urls = Source( srcDescrs )
      .flatMapMerge(srcDescrs.size, _.siteMapXmlEnumerator(ctx))
      // Рендерим каждую ссылку в текст
      .map { _urlTpl(_) }

    // Рендерим ответ: сначала заголовок sitemaps.xml:
    val respBody2: Source[Xml, _] = {
      Source
        .single( beforeUrlsTpl()(ctx) )
        // Затем тело, содержащее ссылки...
        .concat( urls )
        // Футер sitemaps.xml:
        .concat {
          // Форсируем отложенный рендер футера:
          for (_ <- Source.single(1)) yield {
            afterUrlsTpl()(ctx)
          }
        }
    }

    // Возвращаем chunked-ответ с XML.
    val w = implicitly[Writeable[Xml]]
    Ok.sendEntity( HttpEntity.Streamed(
      data          = respBody2.map( w.transform ),
      contentLength = None,
      contentType   = w.contentType
    ))
      .withHeaders(
        CACHE_CONTROL -> s"public, max-age=$SITEMAP_XML_CACHE_TTL_SECONDS"
      )
  }

}
