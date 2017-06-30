package util.seo

import akka.stream.scaladsl.Source
import javax.inject.Inject
import models.crawl.SiteMapUrlT
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

  def sitemapUrlsSrc(): Source[SiteMapUrlT, _] = {
    val srcDescs = SITEMAP_SOURCES
    if (srcDescs.isEmpty) {
      Source.empty
    } else if (srcDescs.tail.isEmpty) {
      // Один источник, перемешивание не требуется
      srcDescs.head.siteMapXmlSrc()
    } else {
      // Два и более источников. Перемешиваем их...
      Source( srcDescs )
        .flatMapMerge( Math.min(10, srcDescs.size), _.siteMapXmlSrc() )
    }
  }

}


/** Интерфейс для контроллеров, которые раздают страницы, подлежащие публикации в sitemap.xml. */
trait SiteMapXmlCtl {

  /** Асинхронно поточно генерировать данные о страницах, подлежащих индексации. */
  def siteMapXmlSrc(): Source[SiteMapUrlT, _]

}


