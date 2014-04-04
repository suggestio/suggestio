package io.suggest.ym.model.common

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 04.04.14 10:51
 * Description: Всякая мелкая утиль для рекламной сети.
 */

/** Типы узлов рекламной сети. */
object AdNetMemberTypes extends Enumeration {
  type AdNetMemberType = Value

  val MART = Value("m")
  val SHOP = Value("s")
  val RESTARAUNT = Value("r")

  /** Супервизор - некий диспетчер, управляющий под-сетью. */
  val ASN_SUPERVISOR = Value("s")
}
