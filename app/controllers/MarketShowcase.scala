package controllers

import controllers.sc._
import io.suggest.event.SNStaticSubscriberDummy
import util._


/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 02.07.14 14:07
 * Description: Контроллер выдачи sio-market.
 * 2014.nov.10: Из-за активного наращивания функционала был разрезан на части, расположенные в controllers.sc.*.
 */
object MarketShowcase extends SioControllerImpl with PlayMacroLogsImpl with SNStaticSubscriberDummy
with ScSiteNode with ScSiteGeo with ScNodeInfo with ScIndexGeo with ScIndexNode with ScSyncSiteGeo
with ScAdsTile with ScFocusedAds with ScNodesList with ScBlockCss with ScSitemapsXml with ScOnlyOneAd
