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
  import io.suggest.ym.ad.ShowLevelsUtil._
  import AdShowLevels._

  /** Дополняем экземпляры Enumeration.Val дополнительными способностями, специфичными для sio-market. */
  abstract protected case class Val(name: String) extends super.Val(name) {
    def getAdnInfoDflt: AdNetMemberInfo
  }

  // TODO Надо бы "= Value", но почему-то он везде этот тип красным подсвечивается.
  type AdNetMemberType = Val

  implicit def value2val(x: Value) = x.asInstanceOf[Val]

  /** Торговый центр. */
  val MART = new Val("m") {
    def getAdnInfoDflt: AdNetMemberInfo = {
      AdNetMemberInfo(
        memberType = this,
        isProducer = true,
        isReceiver = true,
        isSupervisor = false, // 2014.apr.09: Решено, что у ТЦ не должно быть прав супервайзера по объективным причинам.
        isEnabled = true,
        showLevelsInfo = AdnMemberShowLevels(
          in = Map(
            LVL_MEMBER          -> MART_LVL_IN_MEMBER_DFLT,
            LVL_MEMBERS_CATALOG -> MART_LVL_IN_MEMBERS_CATALOG_DFLT,
            LVL_START_PAGE      -> MART_LVL_IN_START_PAGE_DFLT
          ),
          out = Map(LVL_START_PAGE -> MART_LVL_OUT_START_PAGE_DFLT)
        )
      )
    }
  }

  /** Магазин. Обычно арендатор в ТЦ. */
  val SHOP = new Val("s") {
    def getAdnInfoDflt: AdNetMemberInfo = {
      AdNetMemberInfo(
        memberType = this,
        isProducer = true,
        isReceiver = false,
        isSupervisor = false,
        isEnabled = false,
        showLevelsInfo = AdnMemberShowLevels(
          // Магазин не является
          in = Map.empty,
          out = Map(
            LVL_START_PAGE      -> SHOP_LVL_OUT_START_PAGE_DFLT,
            LVL_MEMBERS_CATALOG -> SHOP_LVL_OUT_MEMBER_CATALOG_MAX,
            LVL_MEMBER          -> SHOP_LVL_OUT_MEMBER_DLFT
          )
        )
      )
    }
  }

  /** Генератор лефолтовых экземпляров [[AdNetMemberInfo]]. */
  def getAdnInfoDfltFor(memberType: AdNetMemberType) = memberType.getAdnInfoDflt

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
  val LVL_START_PAGE = Value("d")

  /** Отображать в каталоге продьюсеров. */
  val LVL_MEMBERS_CATALOG = Value("h")

  /** Отображать эту рекламу внутри каталога продьюсера. */
  val LVL_MEMBER = Value("m")

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
    sls.contains(LVL_MEMBER) || sls.contains(LVL_MEMBERS_CATALOG) || (htl && sls.contains(LVL_START_PAGE))
  }

}

