package models

import play.api.data.Form

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 24.12.14 15:18
 */
package object adv {

  /** Тип списка целей для обработки. */
  type ActorTargets_t   = List[MExtTargetInfoFull]

  /** Тип одной формы в списке таргетов. */
  type OneExtTgForm     = Form[(MExtTarget, Option[MExtReturn])]

  type ExtAdvForm       = Form[List[MExtTargetInfo]]

}
