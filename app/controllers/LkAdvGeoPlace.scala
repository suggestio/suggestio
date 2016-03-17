package controllers

import com.google.inject.Inject
import models.GeoIp
import models.jsm.init.MTargets
import models.mproj.ICommonDi
import models.req.IAdProdReq
import play.api.mvc.Result
import util.PlayMacroLogsImpl
import util.acl.{CanAdvertiseAdUtil, CanAdvertiseAd}

import scala.concurrent.Future

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 14.03.16 14:55
  * Description: Контроллер для просто размещений в произвольном месте на карте.
  */
class LkAdvGeoPlace @Inject() (
  override val canAdvAdUtil       : CanAdvertiseAdUtil,
  override val mCommonDi          : ICommonDi
)
  extends SioControllerImpl
  with PlayMacroLogsImpl
  with CanAdvertiseAd
{

  import mCommonDi._

  /**
    * Реакция на запрос страницы размещения карточки в произвольном месте на карте.
    *
    * @param nodeId id карточки.
    * @return 200 OK + страница с формой размещения.
    */
  def forAd(nodeId: String) = CanAdvertiseAdGet(nodeId, U.Lk).async { implicit request =>
    val ipLocFut = GeoIp.geoSearchInfoOpt
    ???
  }

  /** Общий код возврата страницы с формой размещения в месте живёт здесь. */
  def _forAd(rs: Status)(implicit request: IAdProdReq[_]): Future[Result] = {
    for {
      ctxData0 <- request.user.lkCtxDataFut
    } yield {
      implicit val ctxData = ctxData0.copy(
        jsiTgs = Seq(MTargets.AdvGeoPlaceForm)
      )
      ???
    }
  }

}
