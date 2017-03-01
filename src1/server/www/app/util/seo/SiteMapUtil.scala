package util.seo

import akka.stream.scaladsl.Source
import com.google.inject.Inject
import models.crawl.SiteMapUrlT
import models.mctx.Context
import util.showcase.ScSitemapsXml

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 06.04.15 22:02
 * Description:
 */

class SiteMapUtil @Inject() (
  scSitemapsXml     : ScSitemapsXml
) {

  /** Источники для наполнения sitemap.xml */
  def SITEMAP_SOURCES: List[SiteMapXmlCtl] = {
    scSitemapsXml :: Nil
  }

}


/** Интерфейс для контроллеров, которые раздают страницы, подлежащие публикации в sitemap.xml. */
trait SiteMapXmlCtl {

  /** Асинхронно поточно генерировать данные о страницах, подлежащих индексации. */
  def siteMapXmlEnumerator(implicit ctx: Context): Source[SiteMapUrlT, _]

}


