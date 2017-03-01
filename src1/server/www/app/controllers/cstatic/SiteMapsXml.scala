package controllers.cstatic

import akka.stream.scaladsl.Source
import controllers.SioController
import io.suggest.util.logs.MacroLogsImpl
import play.twirl.api.{Xml, XmlFormat}
import util.acl.IIgnoreAuth
import util.seo.SiteMapUtil
import views.xml.static.sitemap._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 17.12.15 11:45
 * Description: Трейт для контроллера для поддержки экшена с раздачей sitemap'ов.
 */
trait SiteMapsXml extends SioController with IIgnoreAuth with MacroLogsImpl {

  import mCommonDi._


  def siteMapUtil: SiteMapUtil

  /** Время кеширования /sitemap.xml ответа на клиенте. */
  private def SITEMAP_XML_CACHE_TTL_SECONDS = if (isDev) 1 else 120


  /**
   * Раздача сайт-мапы.
   * @return sitemap, генерируемый поточно с очень минимальным потреблением RAM.
   */
  def siteMapXml = ignoreAuth() { implicit request =>
    // Собираем асинхронный неупорядоченный источник sitemap-ссылок:
    val urls = siteMapUtil
      .sitemapUrlsSrc()
      // Рендерим каждую ссылку в текст
      .map { _urlTpl(_) }
      .recover { case ex: Throwable =>
        LOGGER.error("siteMapXml: Unable to render url", ex)
        Xml(s"<!-- Stream error occured: ${ex.getClass.getName} -->")
        XmlFormat.empty
      }

    // Рендерим ответ: сначала заголовок sitemaps.xml:
    val respBody2: Source[Xml, _] = {
      Source
        .single( beforeUrlsTpl() )
        // Затем тело, содержащее ссылки...
        .concat( urls )
        // Футер sitemaps.xml:
        .concat {
          Source.single( afterUrlsTpl() )
        }
    }

    // Возвращаем chunked-ответ с XML.
    Ok.chunked(respBody2)
      .withHeaders(
        CACHE_CONTROL -> s"public, max-age=$SITEMAP_XML_CACHE_TTL_SECONDS"
      )
  }

}
