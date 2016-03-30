package util.adv.geo.place

import com.google.inject.{Inject, Singleton}
import models.adv.geo.place.MAgpFormResult
import play.api.data._, Forms._
import util.adv.AdvFormUtil
import util.maps.RadMapFormUtil
import io.suggest.adv.AdvConstants.Period.PERIOD_FN
import io.suggest.adv.AdvConstants.RadMap.RADMAP_FN

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 17.03.16 16:36
  * Description: Утиль для маппингов форм размещения в месте.
  */
@Singleton
class AgpFormUtil @Inject() (
  advFormUtil       : AdvFormUtil,
  radMapFormUtil    : RadMapFormUtil
) {


  /**
    * Маппинг размещения в произвольном месте на карте.
    * @return Маппинг для сборки формы.
    */
  def agpFormM: Mapping[MAgpFormResult] = {
    mapping(
      RADMAP_FN       -> radMapFormUtil.radMapValM,
      PERIOD_FN       -> advFormUtil.advPeriodM
    )
    { MAgpFormResult.apply }
    { MAgpFormResult.unapply }
  }

  def agpForm: Form[MAgpFormResult] = {
    Form( agpFormM)
  }

}
