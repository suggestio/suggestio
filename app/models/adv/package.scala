package models

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 24.12.14 15:18
 */
package object adv {

  type MExtReturn       = MExtReturns.MExtReturn

  /** Тип списка целей для обработки. */
  type ActorTargets_t   = List[MExtTargetInfoFull]

  type MExtService      = MExtServices.T

}
