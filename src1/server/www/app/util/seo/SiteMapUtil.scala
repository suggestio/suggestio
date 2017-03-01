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
 * Description: Утиль для sitemaps.
 */

case class SiteMapUtil @Inject() (
                                   // productIterator: Новые реализации SiteMapXmlCtl можно кидать сюда в произвольном порядке:
                                   scSitemapsXml     : ScSitemapsXml
                                 ) {

  /** Источники для наполнения sitemap.xml */
  def SITEMAP_SOURCES: List[SiteMapXmlCtl] = {
    // Используем аргументы конструктора как список sitemap-источников.
    productIterator
      .collect {
        case smxc: SiteMapXmlCtl => smxc
      }
      .toList
  }

}


/** Интерфейс для контроллеров, которые раздают страницы, подлежащие публикации в sitemap.xml. */
trait SiteMapXmlCtl {

  /** Асинхронно поточно генерировать данные о страницах, подлежащих индексации. */
  def siteMapXmlSrc(implicit ctx: Context): Source[SiteMapUrlT, _]

}


