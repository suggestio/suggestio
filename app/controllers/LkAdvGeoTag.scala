package controllers

import com.google.inject.Inject
import controllers.ctag.NodeTagsEdit
import io.suggest.event.SioNotifierStaticClientI
import io.suggest.model.geo.{Distance, CircleGs, GeoPoint}
import models.maps.MapViewState
import models.{GeoIp, Context2Factory}
import models.adv.gtag.{MAdvFormResult, GtForm_t, MForAdTplArgs}
import models.jsm.init.MTargets
import org.elasticsearch.client.Client
import org.elasticsearch.common.unit.DistanceUnit
import play.api.i18n.MessagesApi
import play.api.mvc.Result
import util.PlayMacroLogsImpl
import util.acl.{RequestWithAdAndProducer, CanAdvertiseAdUtil, CanAdvertiseAd}
import util.tags.{GeoTagsFormUtil, TagsEditFormUtil}
import views.html.lk.adv.gtag._

import scala.concurrent.{Future, ExecutionContext}

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 18.11.15 14:51
  * Description: Контроллер размещения в гео-тегах.
  */
class LkAdvGeoTag @Inject() (
  geoTagsFormUtil                 : GeoTagsFormUtil,
  override val tagsEditFormUtil   : TagsEditFormUtil,
  override val canAdvAdUtil       : CanAdvertiseAdUtil,
  override val _contextFactory    : Context2Factory,
  override val messagesApi        : MessagesApi,
  override implicit val esClient  : Client,
  override implicit val ec        : ExecutionContext,
  override implicit val sn        : SioNotifierStaticClientI
)
  extends SioControllerImpl
  with PlayMacroLogsImpl
  with CanAdvertiseAd
  with NodeTagsEdit
{

  /**
   * Экшен рендера страницы размещения карточки в теге с географией.
   * @param adId id отрабатываемой карточки.
   */
  def forAd(adId: String) = CanAdvertiseAdGet(adId).async { implicit request =>
    val ipLocFut = GeoIp.geoSearchInfoOpt
    val formEmpty = geoTagsFormUtil.advForm

    val formFut = for {
      ipLocOpt <- ipLocFut
    } yield {
      val gp = ipLocOpt
        .flatMap(_.ipGeopoint)
        .getOrElse( GeoPoint(59.93769, 30.30887) )    // Штаб ВМФ СПб, который в центре СПб

      val res = MAdvFormResult(
        tags      = Nil,
        mapState  = MapViewState(gp, zoom = 10),
        circle    = CircleGs(gp, radius = Distance(10000, DistanceUnit.METERS))
      )

      formEmpty.fill(res)
    }

    formFut.flatMap { form =>
      _forAd(form, Ok)
    }
  }

  /**
   * common-код экшенов GET'а и POST'а формы forAdTpl.
   * @param form Маппинг формы.
   * @param rs Статус ответа HTTP.
   * @return Фьючерс с ответом.
   */
  private def _forAd(form: GtForm_t, rs: Status)
                    (implicit request: RequestWithAdAndProducer[_]): Future[Result] = {
    implicit val _jsInitTargets = Seq(MTargets.AdvGtagForm)
    val rargs = MForAdTplArgs(
      mad       = request.mad,
      producer  = request.producer,
      form      = form
    )
    Ok( forAdTpl(rargs) )
  }


  /**
   * Экшен сабмита формы размещения карточки в теге с географией.
   * @param adId id размещаемой карточки.
   * @return 302 see other, 416 not acceptable.
   */
  def forAdSubmit(adId: String) = CanAdvertiseAdPost(adId).async { implicit request =>
    ???
  }

}
