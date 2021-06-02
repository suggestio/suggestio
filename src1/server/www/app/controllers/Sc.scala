package controllers

import javax.inject.Inject
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
import util.blocks.BlkImgMaker
import util.cdn.{CdnUtil, CorsUtil}
import util.geo.GeoIpUtil
import util.img.{DynImgUtil, LogoUtil, WelcomeUtil}
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
final class Sc @Inject() (
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

  import mCommonDi.current.injector

  override lazy val logoUtil = injector.instanceOf[LogoUtil]
  override lazy val welcomeUtil = injector.instanceOf[WelcomeUtil]
  override lazy val bleUtil = injector.instanceOf[BleUtil]
  override lazy val statUtil = injector.instanceOf[StatUtil]
  override lazy val mNodes = injector.instanceOf[MNodes]
  override lazy val scUtil = injector.instanceOf[ShowcaseUtil]
  override lazy val cdnUtil = injector.instanceOf[CdnUtil]
  override lazy val n2NodesUtil = injector.instanceOf[N2NodesUtil]
  override lazy val maybeAuth = injector.instanceOf[MaybeAuth]
  override lazy val blkImgMaker = injector.instanceOf[BlkImgMaker]
  override lazy val dynImgUtil = injector.instanceOf[DynImgUtil]
  override lazy val jdAdUtil = injector.instanceOf[JdAdUtil]
  override lazy val advGeoRcvrsUtil = injector.instanceOf[AdvGeoRcvrsUtil]
  override lazy val scAdSearchUtil = injector.instanceOf[ScAdSearchUtil]
  override lazy val nodesUtil = injector.instanceOf[NodesUtil]
  override lazy val isNodeAdmin = injector.instanceOf[IsNodeAdmin]
  override lazy val canEditAd = injector.instanceOf[CanEditAd]
  override lazy val scSearchUtil = injector.instanceOf[ScSearchUtil]
  override lazy val geoIpUtil = injector.instanceOf[GeoIpUtil]
  override lazy val corsUtil = injector.instanceOf[CorsUtil]
  override val esModel = injector.instanceOf[EsModel]
  private lazy val assets = injector.instanceOf[Assets]
  override val sioControllerApi = injector.instanceOf[SioControllerApi]

  /** Экшен для доступа к ServiceWorker-скрипту выдачи. */
  def swJs(path: String, asset: Assets.Asset) = assets.versioned(path, asset)

}
