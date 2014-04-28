package io.suggest.ym.model.common

import io.suggest.util.MacroLogsImpl
import AdnRights._
import io.suggest.ym.model.MAdnNode

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

  private def unsupportedNodeOp = throw new UnsupportedOperationException("This node type cannot do such action.")


  /**
   * Дополняем экземпляры Enumeration.Val дополнительными способностями, специфичными для sio-market.
   * @param name Исходный строковой id.
   * @param displayAddrOnAds Отображать адрес владельца рекламной карточке на самой рекламной карточке.
   */
  abstract protected case class Val(name: String, displayAddrOnAds: Boolean, slDflt: AdShowLevel) extends super.Val(name) {
    /** Можно ли отображать кнопку просмотра подчинённых узлов? */
    def canViewSlaves: Boolean

    /** Дефолтовая ADN-конфигурация при создании узла. Заливается в поле [[io.suggest.ym.model.MAdnNode.adn]]. */
    def getAdnInfoDflt: AdNetMemberInfo

    /**
     * Добавлен новый узел в качестве подчинённого узла. Нужно внести измения в подчинённый узел.
     * @param parent Существующий узел-супервизор.
     * @param child Подчинённый узел, который скорее всего НЕ имеет своего id.
     * @return true если были внесены изменения в parent-узел.
     */
    def prepareChildForParent(parent: MAdnNode, child: MAdnNode): Boolean = unsupportedNodeOp

    /**
     * Добавлен подчинённый узел. Нужно внести изменения в тело подчинённого узла.
     * @param parent Существующий супервизор.
     * @param child Новый подчинённый узел, имеющий id.
     * @return true, если были внесены изменения в child-узел.
     */
    def updateParentForChild(parent: MAdnNode, child: MAdnNode): Boolean = unsupportedNodeOp
  }


  // TODO Надо бы "= Value", но почему-то он везде этот тип красным подсвечивается.
  type AdNetMemberType = Val

  implicit def value2val(x: Value): AdNetMemberType = x.asInstanceOf[AdNetMemberType]

  /** Торговый центр. */
  val MART = new Val("m", displayAddrOnAds = false, slDflt = AdShowLevels.LVL_START_PAGE) {
    override def canViewSlaves = true
    override def getAdnInfoDflt: AdNetMemberInfo = {
      AdNetMemberInfo(
        memberType = this,
        // 2014.apr.09: Решено, что у ТЦ не должно быть прав супервайзера по объективным причинам.
        rights = Set(PRODUCER, RECEIVER),
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
    // ТЦ при добавлении магазина должен прописывать его в свой producerIds.
    override def updateParentForChild(parent: MAdnNode, child: MAdnNode): Boolean = {
      parent.adn.producerIds += child.id.get
      true
    }
  }

  /** Магазин. Обычно арендатор в ТЦ. */
  val SHOP = new Val("s", displayAddrOnAds = true, slDflt = AdShowLevels.LVL_MEMBER) {
    override def canViewSlaves = false
    override def getAdnInfoDflt: AdNetMemberInfo = {
      AdNetMemberInfo(
        memberType = this,
        rights = Set(PRODUCER),
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
    // Для магазина нормально быть внутри ТЦ. Для этого ему не требуются изменения в состоянии.
    override def prepareChildForParent(parent: MAdnNode, child: MAdnNode) = false
  }

  /** Ресторан в сети ресторанов. */
  val RESTAURANT = new Val("r", displayAddrOnAds = false, slDflt = AdShowLevels.LVL_START_PAGE) {
    override def canViewSlaves = false
    override def getAdnInfoDflt: AdNetMemberInfo = {
      AdNetMemberInfo(
        memberType = this,
        rights = Set(PRODUCER, RECEIVER),
        isEnabled = true,
        showLevelsInfo = AdnMemberShowLevels(
          in = Map(
            LVL_START_PAGE -> SHOP_LVL_OUT_START_PAGE_DFLT
          ),
          out = Map(
            LVL_START_PAGE -> MART_LVL_OUT_START_PAGE_DFLT
          )
        )
      )
    }
    // При добавлении ресторана, супервизор ресторанной сети является для ресторана источником рекламных карточек.
    override def prepareChildForParent(parent: MAdnNode, child: MAdnNode): Boolean = {
      child.adn.producerIds += parent.id.get
      true
    }
  }

  /** Супервайзер сети ресторанов. */
  val RESTAURANT_SUP = new Val("R", displayAddrOnAds = false, slDflt = AdShowLevels.LVL_START_PAGE) {
    override def canViewSlaves = true
    override def getAdnInfoDflt: AdNetMemberInfo = {
      AdNetMemberInfo(
        memberType = this,
        rights = Set(PRODUCER, SUPERVISOR, RECEIVER),
        isEnabled = true,
        showLevelsInfo = AdnMemberShowLevels(
          in = Map(
            LVL_START_PAGE -> MART_LVL_IN_START_PAGE_DFLT
          ),
          out = Map(
            LVL_START_PAGE -> SHOP_LVL_OUT_START_PAGE_DFLT
          )
        )
      )
    }
    // Добавление дочерних элементов -- это норма для супервизора ресторанной сети, но дополнительные действия не требуются.
    override def updateParentForChild(parent: MAdnNode, child: MAdnNode): Boolean = false
  }


  /** Генератор лефолтовых экземпляров [[AdNetMemberInfo]]. */
  def getAdnInfoDfltFor(memberType: AdNetMemberType) = memberType.getAdnInfoDflt

  /** Безопасная версия withName(). */
  def maybeWithName(s: String): Option[AdNetMemberType] = {
    try {
      Some(withName(s))
    } catch {
      case ex: NoSuchElementException => None
    }
  }
}



/** Известные системе типы офферов. */
object AdOfferTypes extends Enumeration {
  type AdOfferType = Value

  val PRODUCT   = Value("p")
  val DISCOUNT  = Value("d")
  val TEXT      = Value("t")
  val RAW       = Value("r")

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

  /**
   * Надстройка над исходным классом-значением.
   * @param name Исходный строковой id enum-элемента.
   * @param visualPrio Визуальный приоритет отображения. Если надо отобразить несколько галочек, то
   *                   они должны отображаться в неком стабильном порядке.
   * @param checkboxCssClass При рендере галочки, она должна иметь этот css-класс.
   */
  protected case class Val(
    name: String,
    visualPrio: Int,
    checkboxCssClass: String
  ) extends super.Val(name)

  implicit def value2val(v: Value) = v.asInstanceOf[Val]

  type AdShowLevel = Val

  /** Отображать на нулевом уровне, т.е. при входе в ТЦ/ресторан и т.д. */
  val LVL_START_PAGE = Val("d", 300, "firstPage-catalog")

  /** Отображать в каталоге продьюсеров. */
  val LVL_MEMBERS_CATALOG = Val("h", 100, "common-catalog")

  /** Отображать эту рекламу внутри каталога продьюсера. */
  val LVL_MEMBER = Val("m", 200, "shop-catalog")


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

