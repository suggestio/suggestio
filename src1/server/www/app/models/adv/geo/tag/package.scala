package models.adv.geo

import play.api.data.Form

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 18.11.15 16:15
 */
package object tag {

  /** Тип для маппинга формы размещения в теге с геогафией. */
  @deprecated("Use MFormS/MFormInit + boopickle instead", "2016.jan.11")
  type AgtForm_t  = Form[MAgtFormResult]

}
