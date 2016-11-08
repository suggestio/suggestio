package controllers

import com.google.inject.Inject
import io.suggest.model.es.MEsUuId
import io.suggest.model.geo.GeoPoint
import models.adv.form.MDatesPeriod
import models.jsm.init.MTargets
import models.madn.mapf.{MAdnMapFormRes, MAdnMapTplArgs}
import models.maps.MapViewState
import models.mctx.Context
import models.mproj.ICommonDi
import models.req.INodeReq
import play.api.data.Form
import play.api.mvc.Result
import util.PlayMacroLogsImpl
import util.acl.IsAdnNodeAdmin
import util.adn.LkAdnMapFormUtil
import util.adv.geo.AdvGeoLocUtil
import util.billing.Bill2Util
import views.html.lk.adn.mapf._

import scala.concurrent.Future

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 02.11.16 11:22
  * Description: Контроллер личного кабинета для связывания узла с точкой/местом на карте.
  * На карте в точках размещаются узлы ADN, и это делается за денежки.
  */
class LkAdnMap @Inject() (
  lkAdnMapFormUtil              : LkAdnMapFormUtil,
  bill2Util                     : Bill2Util,
  advGeoLocUtil                 : AdvGeoLocUtil,
  override val mCommonDi        : ICommonDi
)
  extends SioControllerImpl
  with PlayMacroLogsImpl
  with IsAdnNodeAdmin
{

  import LOGGER._
  import mCommonDi._


  /** Асинхронный детектор начальной точки для карты георазмещения. */
  private def getGeoPoint0(nodeId: String)(implicit request: INodeReq[_]): Future[GeoPoint] = {
    import advGeoLocUtil.Detectors._
    // Ищем начальную точку среди прошлых размещений текущей карточки.
    FromProducerGeoAdvs(
      producerIds  = nodeId :: request.user.personIdOpt.toList
    )
      .orFromReqOrDflt
      // Запустить определение геоточки по обозначенной цепочке.
      .get
  }


  /**
    * Рендер страницы с формой размещения ADN-узла в точке на карте.
    * @param esNodeId id текущего ADN-узла.
    */
  def forNode(esNodeId: MEsUuId) = IsAdnNodeAdminGet(esNodeId, U.Lk).async { implicit request =>
    // TODO Заполнить форму начальными данными: положение карты, начальная точка, начальный период размещения
    val nodeId: String = esNodeId
    val geoPointFut = getGeoPoint0(nodeId)

    val formResFut = for {
      geoPoint <- geoPointFut
    } yield {
      MAdnMapFormRes(
        point    = geoPoint,
        mapState = MapViewState(
          center = geoPoint
        ),
        period   = MDatesPeriod()
      )
    }

    formResFut.flatMap { formRes =>
      val form = lkAdnMapFormUtil.adnMapFormM
        .fill(formRes)

      _forNode(Ok, form)
    }
  }


  /** Рендер страницы с формой узла */
  def _forNode(rs: Status, form: Form[MAdnMapFormRes])(implicit request: INodeReq[_]): Future[Result] = {

    // Собрать контекст для шаблонов. Оно зависит от контекста ЛК, + нужен доп.экшен для запуска js данной формы.
    val ctxFut = for {
      lkCtxData <- request.user.lkCtxDataFut
    } yield {
      implicit val ctx = lkCtxData.copy(
        jsiTgs = Seq(MTargets.AdnMapForm)
      )
      implicitly[Context]
    }

    // Собрать основные аргументы для рендера шаблона
    val rargs = MAdnMapTplArgs(
      mnode = request.mnode,
      form  = form,
      price = bill2Util.zeroPricing
    )

    // Отрендерить результат запроса.
    for {
      ctx <- ctxFut
    } yield {
      val html = AdnMapTpl(rargs)(ctx)
      rs(html)
    }
  }


  /** Сабмит формы размещения узла. */
  def forNodeSubmit(esNodeId: MEsUuId) = IsAdnNodeAdminPost(esNodeId).async { implicit request =>
    ???
  }


  /** Сабмит формы для рассчёт стоимости размещения. */
  def getPriceSubmit(esNodeId: MEsUuId) = IsAdnNodeAdminPost(esNodeId).async { implicit request =>
    ???
  }

}
