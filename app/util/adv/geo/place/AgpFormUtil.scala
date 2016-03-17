package util.adv.geo.place

import com.google.inject.{Inject, Singleton}
import util.adv.AdvFormUtil
import util.maps.RadMapFormUtil

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

}
