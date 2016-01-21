package models.mbill

import de.jollyday.HolidayManager

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 25.12.15 16:41
 * Description: bill v1 контекст с календарями.
 */
case class MCals1Ctx(
  weekend: HolidayManager,
  prime  : HolidayManager
)
