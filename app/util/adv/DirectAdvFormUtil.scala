package util.adv

import com.google.inject.Inject
import models.adv.direct.{OneNodeInfo, FormResult, DirectAdvFormM_t}
import play.api.data._, Forms._
import util.FormUtil.esIdM

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
      "advertise"     -> boolean,
      "showLevel"     -> advFormUtil.adSlsM
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
      "node"   -> nodesM,
      "period" -> advFormUtil.advPeriodM
    )
    { FormResult.apply }
    { FormResult.unapply }
  }

  /** Маппинг формы размещения рекламы на других узлах. */
  def advForm: DirectAdvFormM_t = {
    Form(advFormM)
  }


}
