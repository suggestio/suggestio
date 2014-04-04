package io.suggest.ym.model.common

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

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

  /** Никто. Используется для обозначения посторонних элементов в сети. */
  val NOBODY = Value("n")
}


/** Объект содержит данные по картинке. Данные не индексируются, и их схему можно менять на лету. */
@JsonIgnoreProperties(ignoreUnknown = true)
case class MImgInfo(id: String, meta: Option[MImgInfoMeta] = None) {
  override def hashCode(): Int = id.hashCode()
}
case class MImgInfoMeta(height: Int, width: Int)


/** Известные системе типы офферов. */
object MMartAdOfferTypes extends Enumeration {
  type MMartAdOfferType = Value

  val PRODUCT   = Value("p")
  val DISCOUNT  = Value("d")
  val TEXT      = Value("t")

  def maybeWithName(n: String): Option[MMartAdOfferType] = {
    try {
      Some(withName(n))
    } catch {
      case ex: Exception => None
    }
  }
}

