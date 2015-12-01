package models.adv.bill

import models.SinkShowLevel

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 25.11.15 15:47
 * Description: Модель данных размещения в выдаче кокретного узла.
 */

case class ScRcvr(
  rcvrId  : String,
  sls     : Set[SinkShowLevel]
)
