package controllers

import controllers.sc._
import io.suggest.event.SNStaticSubscriberDummy
import util._
import util.acl._
import views.html.market.showcase._
import models._
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import SiowebEsUtil.client

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 02.07.14 14:07
 * Description: Контроллер выдачи sio-market.
 * 2014.nov.10: Из-за активного наращивания функционала был разрезан на части, расположенные в controllers.sc.*.
 */
object MarketShowcase extends SioController with PlayMacroLogsImpl with SNStaticSubscriberDummy
with ScSiteNode with ScSiteGeo with ScNodeInfo with ScIndexGeo with ScIndexNode with ScSyncSiteGeo
with ScAdsTile with ScFocusedAds with ScNodesList with ScBlockCss with ScSitemapsXml
{

  import LOGGER._

  /**
   * Отрендерить одну указанную карточку как веб-страницу.
   * @param adId id рекламной карточки.
   * @return 200 Ок с отрендеренной страницей-карточкой.
   */
  def standaloneBlock(adId: String) = MaybeAuth.async { implicit request =>
    // TODO Вынести логику read-набега на карточку в отдельный ACL ActionBuilder.
    MAd.getById(adId) map {
      case Some(mad) =>
        val bc: BlockConf = BlocksConf(mad.blockMeta.blockId)
        // TODO Проверять карточку на опубликованность?
        cacheControlShort {
          Ok( bc.renderBlock(mad, blk.RenderArgs(szMult = 1.0F, isStandalone = true)) )
        }

      case None =>
        warn(s"AdId $adId not found.")
        http404AdHoc
    }
  }

}

