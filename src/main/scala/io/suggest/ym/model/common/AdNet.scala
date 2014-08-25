package io.suggest.ym.model.common

import io.suggest.util.MacroLogsImplLazy
import AdnRights._
import io.suggest.ym.model.MAdnNode
import io.suggest.ym.model.common.AdnMemberShowLevels.LvlMap_t
import io.suggest.util.MyConfig.CONFIG

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 04.04.14 10:51
 * Description: Всякая мелкая утиль для рекламной сети.
 */

/** Типы узлов рекламной сети. */
object AdNetMemberTypes extends Enumeration {


  /**
   * Дополняем экземпляры Enumeration.Val дополнительными способностями, специфичными для sio-market.
   * @param name Исходный строковой id.
   */
  abstract protected case class Val(name: String) extends super.Val(name) with ANMTValT {
    /** Дефолтовая ADN-конфигурация при создании узла. Заливается в поле [[io.suggest.ym.model.MAdnNode.adn]]. */
    override def getAdnInfoDflt = {
      AdNetMemberInfo(
        memberType = this,
        rights = adnRights,
        showLevelsInfo = showLevels
      )
    }
  }


  // TODO Надо бы "= Value", но почему-то он везде этот тип красным подсвечивается.
  type AdNetMemberType = Val

  implicit def value2val(x: Value): AdNetMemberType = x.asInstanceOf[AdNetMemberType]


  /** Торговый центр. */
  val MART: AdNetMemberType = new Val("m") with ANMTProducer with ANMTReceiver {
    override def displayAddrOnAds = false
    override def slDflt = AdShowLevels.LVL_START_PAGE

    override def canViewSlaves = true
    // ТЦ при добавлении магазина должен прописывать его в свой producerIds.
    override def updateParentForChild(parent: MAdnNode, child: MAdnNode) = false
  }


  /** Магазин. Обычно арендатор в ТЦ. */
  val SHOP: AdNetMemberType = new Val("s") with ANMTProducer {
    override def displayAddrOnAds = true
    override def slDflt = AdShowLevels.LVL_PRODUCER

    override def canViewSlaves = false
    // Для магазина нормально быть внутри ТЦ. Для этого ему не требуются изменения в состоянии.
    override def prepareChildForParent(parent: MAdnNode, child: MAdnNode) = false
  }


  /** Ресторан в сети ресторанов. */
  val RESTAURANT: AdNetMemberType = new Val("r") with ANMTProducer with ANMTReceiver {
    override def displayAddrOnAds = false
    override def slDflt = AdShowLevels.LVL_START_PAGE
    override def canViewSlaves = false
    // При добавлении ресторана, супервизор ресторанной сети является для ресторана источником рекламных карточек.
    override def prepareChildForParent(parent: MAdnNode, child: MAdnNode): Boolean = false
  }


  /** Супервайзер сети ресторанов. */
  val RESTAURANT_SUP: AdNetMemberType = new Val("R") with ANMTProducer with ANMTSupervisor {
    override def displayAddrOnAds = false
    override def slDflt = AdShowLevels.LVL_START_PAGE
    override def canViewSlaves = true
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

  val BLOCK: AdOfferType = Value("b")

  def maybeWithName(n: String): Option[AdOfferType] = {
    try {
      Some(withName(n))
    } catch {
      case ex: Exception => None
    }
  }
}



sealed trait ANMTValT {
  import AdShowLevels._

  def id: Int

  /** Отображать адрес владельца рекламной карточке на самой рекламной карточке? */
  def displayAddrOnAds: Boolean

  def slDflt: AdShowLevel

  /** Можно ли отображать кнопку просмотра подчинённых узлов? */
  def canViewSlaves: Boolean

  protected def unsupportedNodeOp = throw new UnsupportedOperationException("This node type cannot do such action.")

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

  /** Входящие уровни отображения для внешних рекламодателей. У не-ресиверов тут обычно пусто. */
  def showLevelsIn: LvlMap_t = Map.empty

  /** Исходящие уровни бесплатной публикации. Позволяют галочкой что-то опубликовать, у самого себя. */
  def showLevelsOut: LvlMap_t = Map.empty

  def showLevels = AdnMemberShowLevels(in = showLevelsIn, out = showLevelsOut)
  def adnRights: Set[AdnRight] = Set.empty

  /** Дефолтовая ADN-конфигурация при создании узла. Заливается в поле [[io.suggest.ym.model.MAdnNode.adn]]. */
  def getAdnInfoDflt: AdNetMemberInfo
}


object ANMTReceiver {
  import AdShowLevels._


  // Дефолтовые лимиты уровней для MART и SHOP
  /** Дефолтовая общая ширина выдачи карточек внутри магазинов-арендаторов. */
  private val MART_LVL_IN_MEMBER_DFLT          = CONFIG.getInt("sl.mart.in.lvl_member.dflt") getOrElse 5000

  /** Дефолтовая ширина каталога арендаторов. */
  private val MART_LVL_IN_MEMBERS_CATALOG_DFLT = CONFIG.getInt("mart.show.levels.in.lvl_member_catalog.dflt") getOrElse 1000

  /** Сколько ТЦ максимум может отображать ЧУЖОЙ рекламы на первой странице. */
  private val MART_LVL_IN_START_PAGE_DFLT      = CONFIG.getInt("sl.mart.in.lvl_start_page.dflt") getOrElse 500

  /** Дефолтовое кол-во собственных (исходящих) карточек, которое может публиковать ТЦ. */
  private val MART_LVL_OUT_START_PAGE_DFLT     = CONFIG.getInt("sl.mart.out.lvl_start_page.dflt") getOrElse 2


  val rcvrSlIn: LvlMap_t = Map(
    LVL_PRODUCER          -> MART_LVL_IN_MEMBER_DFLT,
    LVL_CATS -> MART_LVL_IN_MEMBERS_CATALOG_DFLT,
    LVL_START_PAGE      -> MART_LVL_IN_START_PAGE_DFLT
  )

  val rcvrSlOut: LvlMap_t = Map(LVL_START_PAGE -> MART_LVL_OUT_START_PAGE_DFLT)

  /** Нанооптимизация хранимых в памяти карт. Позволяет сложить две карты с учётом того, что одна из них м.б. пустой. */
  def maybeAddMap(map1: LvlMap_t, map2: LvlMap_t): LvlMap_t = {
    if (map1.isEmpty)
      map2
    else if (map2.isEmpty)
      map1
    else map1 ++ map2
  }

}


import ANMTReceiver._

/** Трейт для ресивера. */
sealed trait ANMTReceiver extends ANMTValT {

  /** Входящие уровни отображения для внешних рекламодателей. */
  override def showLevelsIn = {
    maybeAddMap(super.showLevelsIn, rcvrSlIn)
  }

  /** Позволяют ресиверу галочкой что-то опубликовать, у самого себя. */
  override def showLevelsOut: LvlMap_t = {
    maybeAddMap(super.showLevelsOut, rcvrSlOut)
  }

  override def adnRights: Set[AdnRight] = super.adnRights + RECEIVER
}


/** Трейт для продьюсера. */
sealed trait ANMTProducer extends ANMTValT {
  override def adnRights: Set[AdnRight] = super.adnRights + PRODUCER
}


/** Трейт для супервизора. */
sealed trait ANMTSupervisor extends ANMTValT {
  override def adnRights: Set[AdnRight] = super.adnRights + SUPERVISOR

  /** У самого себя публиковать может быть нечего. Но в целом для работы супервизора это надо. */
  override def showLevelsOut: LvlMap_t = {
    maybeAddMap(super.showLevelsOut, ANMTReceiver.rcvrSlOut)
  }
}

