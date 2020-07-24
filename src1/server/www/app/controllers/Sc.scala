package controllers

import javax.inject.{Inject, Named, Singleton}
import controllers.sc._
import io.suggest.es.model.EsModel
import io.suggest.n2.node.MNodes
import io.suggest.util.logs.MacroLogsImpl
import models.mctx.ContextUtil
import models.mproj.ICommonDi
import util.acl._
import util.ad.JdAdUtil
import util.adn.NodesUtil
import util.adr.AdRenderUtil
import util.adv.AdvUtil
import util.adv.geo.{AdvGeoLocUtil, AdvGeoRcvrsUtil}
import util.ble.BleUtil
import util.cdn.{CdnUtil, CorsUtil}
import util.ext.ExtServicesUtil
import util.geo.GeoIpUtil
import util.i18n.JsMessagesUtil
import util.img.{DynImgUtil, IImgMaker, LogoUtil, WelcomeUtil}
import util.n2u.N2NodesUtil
import util.sec.CspUtil
import util.showcase._
import util.stat.{StatCookiesUtil, StatUtil}


/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 02.07.14 14:07
 * Description: Контроллер выдачи sio-market.
 * 2014.nov.10: Из-за активного наращивания функционала был разрезан на части, расположенные в controllers.sc.*.
 */
@Singleton
final class Sc @Inject() (
                     override val logoUtil           : LogoUtil,
                     override val welcomeUtil        : WelcomeUtil,
                     override val bleUtil            : BleUtil,
                     override val statUtil           : StatUtil,
                     override val statCookiesUtil    : StatCookiesUtil,
                     override val mNodes             : MNodes,
                     override val scUtil             : ShowcaseUtil,
                     override val cdnUtil            : CdnUtil,
                     override val n2NodesUtil        : N2NodesUtil,
                     override val cspUtil            : CspUtil,
                     override val getAnyAd           : GetAnyAd,
                     override val maybeAuth          : MaybeAuth,
                     override val advUtil            : AdvUtil,
                     @Named("blk") override val blkImgMaker  : IImgMaker,
                     override val dynImgUtil         : DynImgUtil,
                     override val advGeoLocUtil      : AdvGeoLocUtil,
                     override val jdAdUtil           : JdAdUtil,
                     override val jsMessagesUtil     : JsMessagesUtil,
                     override val ctxUtil            : ContextUtil,
                     override val advGeoRcvrsUtil    : AdvGeoRcvrsUtil,
                     override val scAdSearchUtil     : ScAdSearchUtil,
                     override val nodesUtil          : NodesUtil,
                     override val ignoreAuth         : IgnoreAuth,
                     override val isNodeAdmin        : IsNodeAdmin,
                     override val canEditAd          : CanEditAd,
                     override val scTagsUtil         : ScSearchUtil,
                     override val geoIpUtil          : GeoIpUtil,
                     override val extServicesUtil    : ExtServicesUtil,
                     override val corsUtil           : CorsUtil,
                     override val esModel            : EsModel,
                     assets                          : Assets,
                     override val sioControllerApi   : SioControllerApi,
                     override val mCommonDi          : ICommonDi
                   )
  extends MacroLogsImpl
  with ScSite
  with ScIndex
  with ScAdsTile
  with ScFocusedAds
  with ScOnlyOneAd
  with ScIndexAdOpen
  with ScJsRouter
  with ScSearch
  with ScUniApi
{

  import mCommonDi.current.injector
  override lazy val adRenderUtil = injector.instanceOf[AdRenderUtil]

  /** Экшен для доступа к ServiceWorker-скрипту выдачи. */
  def swJs(path: String, asset: Assets.Asset) = assets.versioned(path, asset)

}
