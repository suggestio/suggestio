package controllers

import javax.inject.{Inject, Named, Singleton}
import controllers.sc._
import io.suggest.es.model.EsModel
import io.suggest.n2.node.MNodes
import io.suggest.util.logs.MacroLogsImpl
import models.mproj.ICommonDi
import util.acl._
import util.ad.JdAdUtil
import util.adn.NodesUtil
import util.adv.geo.AdvGeoRcvrsUtil
import util.ble.BleUtil
import util.cdn.{CdnUtil, CorsUtil}
import util.geo.GeoIpUtil
import util.img.{DynImgUtil, IImgMaker, LogoUtil, WelcomeUtil}
import util.n2u.N2NodesUtil
import util.showcase._
import util.stat.StatUtil


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
                     override val mNodes             : MNodes,
                     override val scUtil             : ShowcaseUtil,
                     override val cdnUtil            : CdnUtil,
                     override val n2NodesUtil        : N2NodesUtil,
                     override val maybeAuth          : MaybeAuth,
                     @Named("blk") override val blkImgMaker  : IImgMaker,
                     override val dynImgUtil         : DynImgUtil,
                     override val jdAdUtil           : JdAdUtil,
                     override val advGeoRcvrsUtil    : AdvGeoRcvrsUtil,
                     override val scAdSearchUtil     : ScAdSearchUtil,
                     override val nodesUtil          : NodesUtil,
                     override val isNodeAdmin        : IsNodeAdmin,
                     override val canEditAd          : CanEditAd,
                     override val scSearchUtil       : ScSearchUtil,
                     override val geoIpUtil          : GeoIpUtil,
                     override val corsUtil           : CorsUtil,
                     override val esModel            : EsModel,
                     assets                          : Assets,
                     override val sioControllerApi   : SioControllerApi,
                     override val mCommonDi          : ICommonDi
                   )
  extends MacroLogsImpl
  with ScIndex
  with ScAdsTile
  with ScFocusedAds
  with ScIndexAdOpen
  with ScSearch
  with ScUniApi
{

  /** Экшен для доступа к ServiceWorker-скрипту выдачи. */
  def swJs(path: String, asset: Assets.Asset) = assets.versioned(path, asset)

}
