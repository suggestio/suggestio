package controllers

import util.PlayMacroLogsImpl
import util.acl.IsSuperuser
import scala.concurrent.ExecutionContext.Implicits.global
import util.SiowebEsUtil.client
import util.event.SiowebNotifier.Implicts.sn
import views.html.sys1.mdr._
import play.api.Play.{current, configuration}
import models._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 19.06.14 10:45
 * Description: Sys Moderation - контроллер, заправляющий s.io-модерацией рекламных карточек.
 */
object SysMdr extends SioController with PlayMacroLogsImpl {

  import LOGGER._

  /** Сколько карточек на одну страницу модерации. */
  val FREE_ADVS_PAGE_SZ: Int = configuration.getInt("mdr.freeAdvs.page.size") getOrElse 10


  /** Отобразить начальную страницу раздела модерации рекламных карточек. */
  def index = IsSuperuser { implicit request =>
    Ok(mdrIndexTpl())
  }

  /** Страница с бесплатно-размещёнными рекламными карточками, подлежащими модерации s.io. */
  def freeAdvs(page: Int) = IsSuperuser.async { implicit request =>
    MAd.findSelfAdvNonMdr(maxResults = FREE_ADVS_PAGE_SZ, offset = page * FREE_ADVS_PAGE_SZ) flatMap { mads =>
      MAdnNodeCache.multigetByIdCached( mads.map(_.producerId) ) map { producers =>
        val prodsMap = producers.map { p => p.id.get -> p }.toMap
        Ok(freeAdvsTpl(mads, prodsMap, page))
      }
    }
  }


  /** Модерация одной карточки. */
  def freeAdvMdr(adId: String) = IsSuperuser.async { implicit request =>
    ???
  }

}
