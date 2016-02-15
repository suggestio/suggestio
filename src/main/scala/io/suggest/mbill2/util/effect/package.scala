package io.suggest.mbill2.util

import slick.dbio.Effect

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>a
  * Created: 15.02.16 12:59
  * Description: Алиасы для комплексных эффектов slick action'ов.
  */
package object effect {

  type RW   = Effect.Read with Effect.Write

  type RWT  = RW with Effect.Transactional

}
