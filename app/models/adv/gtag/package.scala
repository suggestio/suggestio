package models.adv

import play.api.data.Form

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 18.11.15 16:15
 */
package object gtag {

  /** Тип для маппинга формы размещения в теге с геогафией. */
  type GtForm_t  = Form[MAdvFormResult]

}
