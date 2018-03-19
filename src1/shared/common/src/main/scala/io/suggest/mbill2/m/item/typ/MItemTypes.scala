package io.suggest.mbill2.m.item.typ

import boopickle.Default._
import enumeratum.values.{StringEnum, StringEnumEntry}
import japgolly.univeq.UnivEq

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
  case object AdvDirect extends MItemType("a") {
    override def isInterruptable = true
  }

  /** Заказ геотеггинга для карточки. Размещение по шейпу и id узла-тега-ресивера. */
  case object GeoTag extends MItemType("t") {
    override def isInterruptable = true
    override def isTag = true
  }

  /** Покупка размещения в каком-то месте на карте: по геошейпу без ресиверов. */
  case object GeoPlace extends MItemType("g") {
    override def isInterruptable = true
  }

  /** Размещение ADN-узла (магазина/ТЦ/etc) на карте. */
  @deprecated("Смысл замёржился в GeoLocCaptureArea", "2017-06-02")
  case object AdnNodeMap extends MItemType("m") {
    /** Это размещение узлов ЛК на карте. К карточкам это не относится никак. */
    override def sendToMdrOnOrderClose = false
    override def isInterruptable = true
  }

  /** Прямое размещение тега на узле. */
  case object TagDirect extends MItemType("d") {
    override def isInterruptable = true
    override def isTag = true
  }

  /** Юзер просто пополняет sio-баланс, перекачивая на него деньги из внешнего источника денег. */
  case object BalanceCredit extends MItemType("e") {
    /** Это кредитование баланса. Поэтому false. */
    override def isDebt = false
    /** Никакой рекламной составляющей это действо не несёт. */
    override def sendToMdrOnOrderClose = false
    /** Юзер просто закидывает деньги себе на счёт, аппрува для этого не требуется. */
    override def isApprovable = false
    override def isInterruptable = false
  }

  case object GeoLocCaptureArea extends MItemType("l") {
    override def sendToMdrOnOrderClose = true
    override def isInterruptable = true
  }


  /** Только типы item'ов, относящиеся к гео-размещениям. */
  def advGeoTypes     : List[MItemType]     = GeoTag :: GeoPlace :: Nil

  def advDirectTypes  : List[MItemType]     = AdvDirect :: TagDirect :: Nil

  def adnMapTypes     : List[MItemType]     = AdnNodeMap :: GeoLocCaptureArea :: Nil

  def interruptable = values.filter(_.isInterruptable)

  def tagTypes        : List[MItemType]     = GeoTag :: TagDirect :: Nil

  override val values = findValues

}


/** Класс модели. */
sealed abstract class MItemType(override val value: String) extends StringEnumEntry {

  /** Название по каталогу локализованных названий. */
  def nameI18n: String = {
    "Item.type." + value
  }

  /** Является ли ресивером денег CBCA?
    * Для рекламных размещений внутри suggest.io -- она.
    * Для прочих возможных сделок -- нужно анализировать содержимое MItem.rcvrIdOpt.
    */
  def moneyRcvrIsCbca: Boolean = true

  /** Тип item'а относится к рекламным размещениям или иным услугам, отправляемым на модерацию? */
  def sendToMdrOnOrderClose: Boolean = true

  /** @return true когда требуется/подразумевается стадия аппрува s.io в ЖЦ item'а. */
  def isApprovable: Boolean = true

  /** final, чтобы в case object'ах не было перезаписи. */
  override final def toString = value

  /** Цена item'а является долгом/обязательством клиентам перед сервисом?
    * @return true - дебет, т.е. для всяких оплат товаров и услуг.
    *         false - это крЕдит, т.е. источник средств.
    *         Оплаченный item обогащает sio-баланс клиента своей стоимостью.
    */
  def isDebt: Boolean = true

  /** Можно ли "прерывать" item данного типа?
    * Прерывание item'а: это когда в online-режиме происходит коррекция dateEnd с частичным возвратом средств.
    *
    * По идее, изначально допускается прерывание любых adv и adn-item'ов.
    * Но реализовано на том этапе только прерывание adn-item'ов в lk-adn-map-форме при перезаписи размещения.
    * Нельзя прерывать всякие не-sio товары и услуги.
    */
  def isInterruptable: Boolean

  /** Это какой-либо теггинг. */
  def isTag: Boolean = false

}


object MItemType {

  /** Поддержка boopickle.*/
  implicit val mItemTypePickler: Pickler[MItemType] = {
    import MItemTypes._
    compositePickler[MItemType]
      // TODO scala-2.12: возможно, там всё лучше чем сейчас. И все sealed-object'ы сами подцепятся.
      .addConcreteType[AdvDirect.type]
      .addConcreteType[GeoTag.type]
      .addConcreteType[GeoPlace.type]
      .addConcreteType[AdnNodeMap.type]
      .addConcreteType[TagDirect.type]
  }

  implicit def univEq: UnivEq[MItemType] = UnivEq.derive

  def unapplyStrId(x: MItemType): Option[String] = {
    Some(x.value)
  }

}

