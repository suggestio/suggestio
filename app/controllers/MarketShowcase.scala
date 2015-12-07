package controllers

import com.google.inject.Inject
import controllers.sc._
import io.suggest.event.SioNotifierStaticClientI
import models.mproj.MProjectInfo
import models.{Context2Factory, MAdnNodeCache}
import org.elasticsearch.client.Client
import play.api.i18n.MessagesApi
import util._
import util.cdn.CdnUtil
import util.img.{LogoUtil, WelcomeUtil}
import util.n2u.N2NodesUtil
import util.showcase.{ShowcaseUtil, ShowcaseNodeListUtil, ScStatUtil}
import util.stat.StatUtil

import scala.concurrent.ExecutionContext


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
  override val scNlUtil           : ShowcaseNodeListUtil,
  override val scUtil             : ShowcaseUtil,
  override val cdnUtil            : CdnUtil,
  override val n2NodesUtil        : N2NodesUtil,
  override val mNodeCache         : MAdnNodeCache,
  override val mProjectInfo       : MProjectInfo,
  override val errorHandler       : ErrorHandler,
  override val _contextFactory    : Context2Factory,
  override val messagesApi        : MessagesApi,
  override implicit val current   : play.api.Application,
  override implicit val ec        : ExecutionContext,
  override implicit val esClient  : Client,
  override implicit val sn        : SioNotifierStaticClientI
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
