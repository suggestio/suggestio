package controllers

import com.google.inject.Inject
import com.google.inject.name.Named
import controllers.sc._
import io.suggest.model.n2.node.MNodes
import models.im.make.IMaker
import models.mctx.ContextUtil
import models.mproj.ICommonDi
import util.PlayMacroLogsImpl
import util.adn.NodesUtil
import util.adr.AdRenderUtil
import util.ble.BleUtil
import util.cdn.CdnUtil
import util.ext.ExtServicesUtil
import util.geo.GeoIpUtil
import util.img.{LogoUtil, WelcomeUtil}
import util.n2u.N2NodesUtil
import util.showcase.{ScMapUtil, _}
import util.stat.StatCookiesUtil


/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 02.07.14 14:07
 * Description: Контроллер выдачи sio-market.
 * 2014.nov.10: Из-за активного наращивания функционала был разрезан на части, расположенные в controllers.sc.*.
 */
class Sc @Inject() (
  override val logoUtil           : LogoUtil,
  override val welcomeUtil        : WelcomeUtil,
  override val bleUtil            : BleUtil,
  override val scStatUtil         : ScStatUtil,
  override val statCookiesUtil    : StatCookiesUtil,
  override val mNodes             : MNodes,
  override val scNlUtil           : ShowcaseNodeListUtil,
  override val scUtil             : ShowcaseUtil,
  override val adRenderUtil       : AdRenderUtil,
  override val cdnUtil            : CdnUtil,
  override val n2NodesUtil        : N2NodesUtil,
  @Named("blk") override val blkImgMaker  : IMaker,
  override val scMapUtil          : ScMapUtil,
  override val ctxUtil            : ContextUtil,
  override val scAdSearchUtil     : ScAdSearchUtil,
  override val nodesUtil          : NodesUtil,
  override val scTagsUtil         : ScTagsUtil,
  override val geoIpUtil          : GeoIpUtil,
  override val extServicesUtil    : ExtServicesUtil,
  override val mCommonDi          : ICommonDi
)
  extends SioControllerImpl
  with PlayMacroLogsImpl
  with ScSiteGeo
  with ScIndex
  with ScSyncSite
  with ScAdsTile
  with ScFocusedAdsV2
  with ScNodesList
  with ScBlockCss
  with ScOnlyOneAd
  with ScIndexAdOpen
  with ScJsRouter
  with ScTags
  with ScRemoteError
  with ScMap
