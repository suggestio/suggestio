package io.suggest.mbill2.m.item.typ

import enumeratum.values.{StringEnum, StringEnumEntry}
import io.suggest.enum2.EnumeratumUtil
import japgolly.univeq.UnivEq
import play.api.libs.json.Format
import japgolly.univeq._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 05.04.17 12:44
  * Description: Enumeration для типов item'ов.
  */
object MItemTypes extends StringEnum[MItemType] {

  /**
    * Прямое размещение карточки прямо на каком-либо узле (используя id узла).
    * Это было самый первый тип размещения в suggest.io.
    * Скорее всего, этот же тип будет для размещения в маячках и группах маячков.
    */
  case object AdvDirect extends MItemType("a")

  /** Заказ геотеггинга для карточки. Размещение по шейпу и id узла-тега-ресивера. */
  case object GeoTag extends MItemType("t")

  /** Покупка размещения в каком-то месте на карте: по геошейпу без ресиверов. */
  case object GeoPlace extends MItemType("g")

  /** Прямое размещение тега на узле. */
  case object TagDirect extends MItemType("d")

  /** Юзер просто пополняет sio-баланс, перекачивая на него деньги из внешнего источника денег. */
  case object BalanceCredit extends MItemType("e")

  /** ADN-узел занимает площадь геолокацию на карте. */
  case object GeoLocCaptureArea extends MItemType("l")

  /** Тег для локации ADN-узла: по аналогии с гео-тегом, но не для карточки. */
  case object LocationTag extends MItemType("o")


  override val values = findValues


  /** Только типы item'ов, относящиеся к гео-размещениям. */
  def advGeoTypes     : List[MItemType]     = GeoTag :: GeoPlace :: Nil

  def advDirectTypes  : List[MItemType]     = AdvDirect :: TagDirect :: Nil

  def adnMapTypes     : List[MItemType]     = GeoLocCaptureArea :: LocationTag :: Nil

  /** Типы, допустимые к использованию в модели MAdvDeclKey. */
  def advDeclTypes    : List[MItemType]     = AdvDirect :: Nil

  def interruptable   = values.filter(_.isInterruptable)

  def tagTypes        : List[MItemType]     = GeoTag :: TagDirect :: LocationTag :: Nil

  /** Для каких типов разрешена модерация уровная ЛК юзера? */
  def userMdrAllowed  : List[MItemType]     = advDirectTypes

}


/** Класс модели. */
sealed abstract class MItemType(override val value: String) extends StringEnumEntry {

  /** Название по каталогу локализованных названий. */
  def nameI18n: String = {
    "Item.type." + value
  }

  /** final, чтобы в case object'ах не было перезаписи. */
  override final def toString = value

}


object MItemType {

  @inline implicit def univEq: UnivEq[MItemType] = UnivEq.derive

  def unapplyStrId(x: MItemType): Option[String] = {
    Some(x.value)
  }

  implicit def mItemTypeFormat: Format[MItemType] = {
    EnumeratumUtil.valueEnumEntryFormat( MItemTypes )
  }


  implicit class ItemTypeOpsExt(val itype: MItemType) extends AnyVal {

    /** Является ли ресивером денег только CBCA?
      * Для рекламных гео-размещений внутри suggest.io -- она.
      * 2018-10-07 Для платных размещений в узлах - распределение с учётом тарифа (комиссии) целевого узла.
      */
    def moneyRcvrOnlyCbca: Boolean = {
      itype match {
        case MItemTypes.GeoLocCaptureArea |
             MItemTypes.GeoTag |
             MItemTypes.GeoPlace =>
          true
        case _ => false
      }
    }

    /** Можно ли "прерывать" item данного типа?
      * Прерывание item'а: это когда в online-режиме происходит коррекция dateEnd с частичным возвратом средств.
      *
      * По идее, изначально допускается прерывание любых adv и adn-item'ов.
      * Но реализовано на том этапе только прерывание adn-item'ов в lk-adn-map-форме при перезаписи размещения.
      * Нельзя прерывать всякие не-sio товары и услуги.
      */
    def isInterruptable: Boolean =
      !_isBalanceCredit

    /** Это какой-либо теггинг? */
    def isTag: Boolean = {
      itype match {
        case MItemTypes.GeoTag |
             MItemTypes.TagDirect => true
        case _ => false
      }
    }

    /** Цена item'а является долгом/обязательством клиентам перед сервисом?
      * @return true - дебет, т.е. для всяких оплат товаров и услуг.
      *         false - это крЕдит, т.е. источник средств.
      *         Оплаченный item обогащает sio-баланс клиента своей стоимостью.
      */
    def isDebt: Boolean =
      !_isBalanceCredit // Это кредитование баланса. Поэтому false.

    private def _isBalanceCredit: Boolean =
      itype ==* MItemTypes.BalanceCredit

    def sendToMdrOnOrderClose = {
      itype match {
        case MItemTypes.GeoLocCaptureArea |
             MItemTypes.BalanceCredit => false
        case _ => true
      }
    }

    /** @return true когда требуется/подразумевается стадия аппрува s.io в ЖЦ item'а. */
    def isApprovable: Boolean =
      !_isBalanceCredit  // Юзер просто закидывает деньги себе на счёт, аппрува для этого не требуется.

  }

}

