package io.suggest.ym.model.common

import io.suggest.util.MacroLogsImpl

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



/** Известные системе типы офферов. */
object AdOfferTypes extends Enumeration {
  type AdOfferType = Value

  val PRODUCT   = Value("p")
  val DISCOUNT  = Value("d")
  val TEXT      = Value("t")

  def maybeWithName(n: String): Option[AdOfferType] = {
    try {
      Some(withName(n))
    } catch {
      case ex: Exception => None
    }
  }
}


/** Уровни отображения рекламы. Используется как bitmask, но через денормализацию поля. */
object AdShowLevels extends Enumeration with MacroLogsImpl {
  import LOGGER._
  import scala.collection.JavaConversions._

  type AdShowLevel = Value

  /** Отображать на нулевом уровне, т.е. при входе в ТЦ/ресторан и т.д. */
  val LVL_RECEIVER_TOP = Value("d")

  /** Отображать в каталоге продьюсеров. */
  val LVL_PRODUCERS_CATALOG = Value("h")

  /** Отображать эту рекламу внутри каталога продьюсера. */
  val LVL_PRODUCER = Value("m")

  def maybeWithName(n: String): Option[AdShowLevel] = {
    try {
      Some(withName(n))
    } catch {
      case _: Exception => None
    }
  }

  /** Десериализатор значений из самых примитивных типов и коллекций. */
  val deserializeLevelsFrom: PartialFunction[Any, Set[AdShowLevel]] = {
    case v: java.lang.Iterable[_] =>
      v.foldLeft[List[AdShowLevel]] (Nil) { (acc, slRaw) =>
        AdShowLevels.maybeWithName(slRaw.toString) match {
          case Some(sl) => sl :: acc
          case None =>
            warn(s"Unable to deserialize show level '$slRaw'. Possible levels are: ${AdShowLevels.values.mkString(", ")}")
            acc
        }
      }.toSet
  }

  /**
   * Является ли рекламная карточка ТЦ отображаемой где-либо?
   * @param sls Список уровней отображения рекламной карточки.
   * @return true, если карточка опубликована где-либо. Иначе false.
   */
  def isShownMartAd(sls: Set[AdShowLevel]) = !sls.isEmpty

  /**
   * Является ли рекламная карточка магазина отображаемой?
   * @param htl Есть ли у магазина top level access?
   * @param sls Уровни рекламной карточки.
   * @return true - если карточка где-либо опубликована. Иначе false.
   */
  def isShownShopAd(htl: Boolean, sls: Set[AdShowLevel]): Boolean = {
    sls.contains(LVL_PRODUCER) || sls.contains(LVL_PRODUCERS_CATALOG) || (htl && sls.contains(LVL_RECEIVER_TOP))
  }

}

