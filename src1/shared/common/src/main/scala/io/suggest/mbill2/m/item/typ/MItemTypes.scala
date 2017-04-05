package io.suggest.mbill2.m.item.typ

import enumeratum._
import io.suggest.common.menum.EnumeratumApply
import io.suggest.primo.IStrId

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 05.04.17 12:44
  * Description: Enumeration для типов item'ов.
  */
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

  override final def toString = super.toString

}


object MItemTypes extends EnumeratumApply[MItemType] {

  /**
    * Прямое размещение карточки прямо на каком-либо узле (используя id узла).
    * Это было самый первый тип размещения в suggest.io.
    * Скорее всего, этот же тип будет для размещения в маячках и группах маячков.
    */
  case object AdvDirect extends MItemType {
    override def strId: String = "a"
  }

  /** Заказ геотеггинга для карточки. Размещение по шейпу и id узла-тега-ресивера. */
  case object GeoTag extends MItemType {
    override def strId = "t"
  }

  /** Покупка размещения в каком-то месте на карте: по геошейпу без ресиверов. */
  case object GeoPlace extends MItemType {
    override def strId: String = "g"
  }

  /** Размещение ADN-узла (магазина/ТЦ/etc) на карте. */
  case object AdnNodeMap extends MItemType {
    override def strId = "m"
  }

  /** Прямое размещение тега на узле. */
  case object TagDirect extends MItemType {
    override def strId = "d"
  }

  /** Покупка срочного доступа к внешнему размещению (разовая абонплата). */
  //val AdvExtFee         : T = new Val("e")


  /** Типы, относящиеся к рекламным размещениям. */
  // TODO Перенести TagDirect в advDirectTypes, если это безопасно.
  def advTypes                = advGeoTypes reverse_::: advDirectTypes
  def advTypesIds             = onlyIds( advTypes )

  /** Только типы item'ов, относящиеся к гео-размещениям. */
  def advGeoTypes             = GeoTag :: GeoPlace :: Nil
  def advGeoTypeIds           = onlyIds( advGeoTypes )

  def advDirectTypes          = AdvDirect :: TagDirect :: Nil


  override val values = findValues

}
