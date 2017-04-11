package io.suggest.bill.price.dsl

import boopickle.Default._
import enumeratum._
import io.suggest.primo.IStrId

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 10.04.17 15:28
  * Description: Модели типов причины.
  * Причина начисления заворачивается в доп.класс, чтобы можно передавать какие-то пояснения или аргументы.
  */


object MReasonType {

  /** Поддержка бинарной сериализации. */
  implicit val mReasonTypePickler: Pickler[MReasonType] = {
    import MReasonTypes._
    // TODO 2.12 Организовать с помощью sealed. В scala-2.12 должны были уже починить.
    compositePickler[MReasonType]
      .addConcreteType[OnMainScreen.type]
      .addConcreteType[GeoSq.type]
      .addConcreteType[BlockModulesCount.type]
      .addConcreteType[Tag.type]
  }

}


/** Модель типа причины. */
sealed abstract class MReasonType extends EnumEntry with IStrId {

  override final def strId = toString

  /** Граничная причина, после которой идёт погружение на уровень item'а и его дней. */
  def isItemType: Boolean = false

}


/** Модель типов причин тарификации. */
object MReasonTypes extends Enum[MReasonType] {

  /** Тарифное начисление за размещение на главном экране. */
  case object OnMainScreen extends MReasonType {
    override def toString = "oms"
    override def isItemType = true
  }

  /** Причина начисления - географическая площадь. */
  case object GeoSq extends MReasonType {
    override def toString = "geo"
  }

  /** Накидывание цены за фактическую площадь карточки в плитке. */
  case object BlockModulesCount extends MReasonType {
    override def toString = "bmc"
  }

  /** Накидывают за тег. */
  case object Tag extends MReasonType {
    override def toString = "tag"
    override def isItemType = true
  }

  /** Накидывают за прямое размещение на каком-то узле-ресивере. */
  case object Rcvr extends MReasonType {
    override def toString = "rcvr"
  }

  /** Перепись всех допустимых инстансов. */
  override val values = findValues

}


