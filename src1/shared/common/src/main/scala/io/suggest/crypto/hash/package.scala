package io.suggest.crypto

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 02.10.17 19:00
  */
package object hash {

  /** Тип одной пары типа хеша и hex-значения */
  type HashHex = (MHash, String)

  /** Тип карты хешей и их hex-значений. */
  type HashesHex = Map[MHash, String]

}
