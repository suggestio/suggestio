package controllers

import com.google.inject.Inject
import controllers.ctag.NodeTagsEdit
import io.suggest.event.SioNotifierStaticClientI
import models.Context2Factory
import org.elasticsearch.client.Client
import play.api.i18n.MessagesApi
import util.PlayMacroLogsImpl
import util.acl.{CanAdvertiseAdUtil, CanAdvertiseAd}
import util.tags.TagsEditFormUtil
import views.html.lk.adv.gtag._

import scala.concurrent.ExecutionContext

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 18.11.15 14:51
  * Description: Контроллер размещения в гео-тегах.
  */
class LkAdvGeoTag @Inject() (
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
    ???
  }

}
