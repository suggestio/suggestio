package controllers

import com.google.inject.Inject
import controllers.sc._
import models.im.logo.LogoUtil
import play.api.i18n.MessagesApi
import util._


/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 02.07.14 14:07
 * Description: Контроллер выдачи sio-market.
 * 2014.nov.10: Из-за активного наращивания функционала был разрезан на части, расположенные в controllers.sc.*.
 */
class MarketShowcase @Inject() (
  override val messagesApi  : MessagesApi,
  override val logoUtil     : LogoUtil
)
  extends SioControllerImpl
  with PlayMacroLogsImpl
  with ScSiteNode
  with ScSiteGeo
  with ScNodeInfo
  with ScIndexGeo
  with ScIndexNode
  with ScSyncSiteGeo
  with ScAdsTile
  with ScFocusedAdsV2
  with ScNodesList
  with ScBlockCss
  with ScOnlyOneAd
  with ScIndexAdOpen
  with ScJsRouter
  with ScTags
