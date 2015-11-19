package controllers

import com.google.inject.Inject
import controllers.ctag.NodeTagsEdit
import io.suggest.event.SioNotifierStaticClientI
import models.Context2Factory
import models.adv.gtag.{GtForm_t, MForAdTplArgs}
import models.jsm.init.MTargets
import org.elasticsearch.client.Client
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
    val form = geoTagsFormUtil.advForm
    _forAd(form, Ok)
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
