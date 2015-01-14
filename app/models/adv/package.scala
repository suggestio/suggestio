package models

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 24.12.14 15:18
 * Description: Быстрый доступ к типам моделей.
 */
package object adv {

  type MExtService      = MExtServices.MExtService

  type MExtReturn       = MExtReturns.MExtReturn

  /** Тип списка целей для обработки. */
  type ActorTargets_t   = List[MExtTargetInfoFull]

}
