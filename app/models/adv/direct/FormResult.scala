package models.adv.direct

import org.joda.time.LocalDate

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 03.12.15 16:25
 * Description: Результат маппинга формы adv direct.
 */
case class FormResult(
  nodes     : List[OneNodeInfo],
  period    : (LocalDate, LocalDate)
)
