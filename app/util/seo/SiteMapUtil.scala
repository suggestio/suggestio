package util.seo

import models.Context
import models.crawl.SiteMapUrlT
import play.api.libs.iteratee.Enumerator
import util.showcase.ScSitemapsXml

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 06.04.15 22:02
 * Description:
 */

object SiteMapUtil {

  /** Источники для наполнения sitemap.xml */
  def SITEMAP_SOURCES: Seq[SiteMapXmlCtl] = Seq(
    new ScSitemapsXml,
    new MarketSiteMapXml
  )

}


/** Интерфейс для контроллеров, которые раздают страницы, подлежащие публикации в sitemap.xml. */
trait SiteMapXmlCtl {
  /** Асинхронно поточно генерировать данные о страницах, подлежащих индексации. */
  def siteMapXmlEnumerator(implicit ctx: Context): Enumerator[SiteMapUrlT]
}


