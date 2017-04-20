package util.adn.mapf

import com.google.inject.Inject
import io.suggest.adn.mapf.AdnMapFormConstants.Fields._
import models.madn.mapf.MAdnMapFormRes
import play.api.data.Forms._
import play.api.data._
import util.FormUtil
import util.adv.AdvFormUtil
import util.maps.MapFormUtil

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 02.11.16 11:25
  * Description: Утиль для формы размещения узла ADN на карте.
  * Пока делаем, чтобы точка была только одна максимум. Это упростит ряд вещей.
  *
  * Будем всячески избегать ситуации в проекте, когда точек узла может быть больше одной.
  */
class LkAdnMapFormUtil @Inject() (
  mapFormUtil       : MapFormUtil,
  advFormUtil       : AdvFormUtil
) {

  /** Form-маппинг для [[MAdnMapFormRes]]. */
  def adnMapFormResM: Mapping[MAdnMapFormRes] = {
    mapping(
      POINT_FN      -> FormUtil.geoPointM,
      STATE_FN      -> mapFormUtil.mapStateM,
      PERIOD_FN     -> advFormUtil.advPeriodM,
      TZ_OFFSET_FN  -> number
    )
    { MAdnMapFormRes.apply }
    { MAdnMapFormRes.unapply }
  }

  /** Маппинг для формы размещения ADN-узла на карте. */
  def adnMapFormM: Form[MAdnMapFormRes] = {
    Form(adnMapFormResM)
  }

}
