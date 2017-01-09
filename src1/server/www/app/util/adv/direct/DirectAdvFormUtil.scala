package util.adv.direct

import com.google.inject.Inject
import io.suggest.adv.AdvConstants
import models.adv.direct.{DirectAdvFormM_t, FormResult, OneNodeInfo}
import play.api.data.Forms._
import play.api.data._
import util.FormUtil.esIdM
import util.adv.AdvFormUtil

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 03.12.15 14:09
 * Description: Утиль для маппингов формы прямого размещения.
 */
class DirectAdvFormUtil @Inject() (
  advFormUtil: AdvFormUtil
) {

  /** Маппинг данных по одному узлу. */
  def nodeM: Mapping[OneNodeInfo] = {
    mapping(
      "adnId"         -> esIdM,
      "advertise"     -> boolean
    )
    { OneNodeInfo.apply }
    { OneNodeInfo.unapply }
  }

  /** Маппинг списка нод. */
  def nodesM: Mapping[List[OneNodeInfo]] = {
    list(nodeM)
      .transform [List[OneNodeInfo]] (_.filter(_.isAdv), identity)
  }

  /** Маппинг всей формы. */
  def advFormM: Mapping[FormResult] = {
    mapping(
      "node"                 -> nodesM,
      AdvConstants.PERIOD_FN -> advFormUtil.advPeriodM
    )
    { FormResult.apply }
    { FormResult.unapply }
  }

  /** Маппинг формы размещения рекламы на других узлах. */
  def advForm: DirectAdvFormM_t = {
    Form(advFormM)
  }


}
