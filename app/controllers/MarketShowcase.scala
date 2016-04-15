package controllers

import com.google.inject.Inject
import controllers.sc._
import io.suggest.model.n2.node.MNodes
import models.mproj.{ICommonDi, MProjectInfo}
import util._
import util.cdn.CdnUtil
import util.img.{AdRenderUtil, LogoUtil, WelcomeUtil}
import util.n2u.N2NodesUtil
import util.showcase.{ScStatUtil, ShowcaseNodeListUtil, ShowcaseUtil}
import util.stat.StatUtil


/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 02.07.14 14:07
 * Description: Контроллер выдачи sio-market.
 * 2014.nov.10: Из-за активного наращивания функционала был разрезан на части, расположенные в controllers.sc.*.
 */
class MarketShowcase @Inject() (
  override val logoUtil           : LogoUtil,
  override val welcomeUtil        : WelcomeUtil,
  override val scStatUtil         : ScStatUtil,
  override val statUtil           : StatUtil,
  override val mNodes             : MNodes,
  override val scNlUtil           : ShowcaseNodeListUtil,
  override val scUtil             : ShowcaseUtil,
  override val adRenderUtil       : AdRenderUtil,
  override val cdnUtil            : CdnUtil,
  override val n2NodesUtil        : N2NodesUtil,
  override val mCommonDi          : ICommonDi,
  override val mProjectInfo       : MProjectInfo
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
  with ScRemoteError
