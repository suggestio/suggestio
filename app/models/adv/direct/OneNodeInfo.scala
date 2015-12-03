package models.adv.direct

import models.AdShowLevel

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 03.12.15 16:24
 * Description: Промежуточная модель для данных маппинга.
 */
case class OneNodeInfo(
  adnId : String,
  isAdv : Boolean,
  sls   : Set[AdShowLevel]
)
