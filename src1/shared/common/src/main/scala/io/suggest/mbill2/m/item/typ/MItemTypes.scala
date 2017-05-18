package io.suggest.mbill2.m.item.typ

import boopickle.Default._
import enumeratum._
import io.suggest.common.menum.EnumeratumApply
import io.suggest.primo.IStrId

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 05.04.17 12:44
  * Description: Enumeration для типов item'ов.
  */
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

}


/** Класс модели. */
sealed abstract class MItemType extends EnumEntry with IStrId {

  /** Название по каталогу локализованных названий. */
  def nameI18n: String = {
    "Item.type." + strId
  }

  /** Является ли ресивером денег CBCA?
    * Для рекламных размещений внутри suggest.io -- она.
    * Для прочих возможных сделок -- нужно анализировать содержимое MItem.rcvrIdOpt.
    */
  def moneyRcvrIsCbca: Boolean = true

  /** Тип item'а относится к рекламным размещениям карточек и тегам к ним? */
  def isAdv: Boolean = true

  /** @return true когда требуется/подразумевается стадия аппрува s.io в ЖЦ item'а. */
  def isApprovable: Boolean = true

  /** final, чтобы в case object'ах не было перезаписи. */
  override final def toString = super.toString

  /** Цена item'а является долгом/обязательством клиентам перед сервисом?
    * @return true - дебет, т.е. для всяких оплат товаров и услуг.
    *         false - это крЕдит, т.е. источник средств.
    *         Оплаченный item обогащает sio-баланс клиента своей стоимостью.
    */
  def isDebt: Boolean = true

}


/** Статическая модель всех допустимых типов item'ов. */
object MItemTypes extends EnumeratumApply[MItemType] {

  /**
    * Прямое размещение карточки прямо на каком-либо узле (используя id узла).
    * Это было самый первый тип размещения в suggest.io.
    * Скорее всего, этот же тип будет для размещения в маячках и группах маячков.
    */
  case object AdvDirect extends MItemType {
    override def strId = "a"
  }

  /** Заказ геотеггинга для карточки. Размещение по шейпу и id узла-тега-ресивера. */
  case object GeoTag extends MItemType {
    override def strId = "t"
  }

  /** Покупка размещения в каком-то месте на карте: по геошейпу без ресиверов. */
  case object GeoPlace extends MItemType {
    override def strId = "g"
  }

  /** Размещение ADN-узла (магазина/ТЦ/etc) на карте. */
  case object AdnNodeMap extends MItemType {
    override def strId = "m"
    /** Это размещение узлов ЛК на карте. К карточкам это не относится никак. */
    override def isAdv = false
  }

  /** Прямое размещение тега на узле. */
  case object TagDirect extends MItemType {
    override def strId = "d"
  }

  /** Юзер просто пополняет sio-баланс, перекачивая на него деньги из внешнего источника денег. */
  case object BalanceCredit extends MItemType {
    override def strId = "e"
    /** Это кредитование баланса. Поэтому false. */
    override def isDebt = false
    /** Никакой рекламной составляющей это действо не несёт. */
    override def isAdv = false
    /** Юзер просто закидывает деньги себе на счёт, аппрува для этого не требуется. */
    override def isApprovable = false
  }

  case object GeoLocCaptureArea extends MItemType {
    override def strId = "l"
    override def isAdv = false
  }


  /** Только типы item'ов, относящиеся к гео-размещениям. */
  def advGeoTypes             = GeoTag :: GeoPlace :: Nil
  def advGeoTypeIds           = onlyIds( advGeoTypes )

  def advDirectTypes          = AdvDirect :: TagDirect :: Nil

  def adnMapTypes             = AdnNodeMap :: GeoLocCaptureArea :: Nil

  override val values = findValues

}
