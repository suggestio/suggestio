package util.seo

import com.google.inject.Inject
import controllers.routes
import models.Context
import models.crawl.{SiteMapUrlT, SiteMapUrl, ChangeFreqs}
import models.mproj.MProjectInfo
import play.api.libs.iteratee.Enumerator
import play.api.mvc.Call

import scala.concurrent.ExecutionContext

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 06.04.15 22:13
 * Description: sitemap-утиль для информационного раздела /market/
 */
class MarketSiteMapXml @Inject() (
  mProjectInfo            : MProjectInfo,
  implicit private val ec : ExecutionContext
)
  extends SiteMapXmlCtl
{

  /** Асинхронно поточно генерировать данные о страницах, подлежащих индексации. */
  override def siteMapXmlEnumerator(implicit ctx: Context): Enumerator[SiteMapUrlT] = {
    Enumerator[Call](
      routes.Market.marketBooklet()
    ) map { call =>
      SiteMapUrl(
        loc = ctx.SC_URL_PREFIX + call.url,
        lastMod = Some( mProjectInfo.PROJECT_CODE_LAST_MODIFIED.toLocalDate ),
        changeFreq = Some( ChangeFreqs.weekly )
      )
    }
  }

}
