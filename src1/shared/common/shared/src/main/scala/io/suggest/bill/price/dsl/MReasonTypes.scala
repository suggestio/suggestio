package io.suggest.bill.price.dsl

import enumeratum.values.{StringEnum, StringEnumEntry}
import io.suggest.enum2.EnumeratumUtil
import io.suggest.i18n.MsgCodes
import io.suggest.mbill2.m.item.typ.MItemTypes
import japgolly.univeq.UnivEq
import play.api.libs.json.Format

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 10.04.17 15:28
  * Description: Модели типов причины.
  * Причина начисления заворачивается в доп.класс, чтобы можно передавать какие-то пояснения или аргументы.
  */


/** Модель типов причин тарификации. */
object MReasonTypes extends StringEnum[MReasonType] {


  /** Тарифное начисление за размещение на главном экране. */
  case object OnMainScreen extends MReasonType("oms") {
    override def isItemLevel  = true
    override def msgCodeI18n  = MsgCodes.`Adv.on.main.screen`
  }


  /** Причина начисления - географическая площадь. */
  case object GeoArea extends MReasonType("geo") {
    override def msgCodeI18n  = MsgCodes.`Coverage.area`

    /** Накидывание за гео-покрытие идёт поверх каких-то item'ов. */
    override def isItemLevel = false

  }


  /** Накидывание цены за фактическую площадь карточки в плитке. */
  case object BlockModulesCount extends MReasonType("bmc") {
    override def msgCodeI18n  = MsgCodes.`Ad.area.modules.count`
    /** Кол-во блоков не является оплачиваемым, а является просто множителем для других item-причин. */
    override def isItemLevel = false
  }


  /** Накидывают за тег. */
  case object Tag extends MReasonType("tag") {
    override def isItemLevel  = true
    override def msgCodeI18n  = MsgCodes.`Tag`
  }

  /** Накидывают за прямое размещение на каком-то узле-ресивере. */
  case object Rcvr extends MReasonType("rcvr") {
    /** Это не является item-уровнем. item'ами являются теги и OnMainScreen в underlying-термах. */
    override def isItemLevel = false
    override def msgCodeI18n  = MItemTypes.AdvDirect.nameI18n

  }


  /** Начисление за размещение ADN-узла на карте геолокации пользователей. */
  case object GeoLocCapture extends MReasonType("GLC") {
    override def msgCodeI18n  = MsgCodes.`Users.geo.location.capturing`
    override def isItemLevel  = true
  }


  // -------------------------------------------------------

  /** Перепись всех допустимых инстансов. */
  override val values = findValues

}


/** Модель типа причины. */
sealed abstract class MReasonType(override val value: String) extends StringEnumEntry {

  /** Граничная причина, после которой идёт погружение на уровень item'а и его дней. */
  def isItemLevel: Boolean

  /** Код наименования по messages. */
  def msgCodeI18n: String

  override final def toString = value

}


object MReasonType {

  import boopickle.Default._
  /** Поддержка бинарной сериализации. */
  implicit val mReasonTypePickler: Pickler[MReasonType] = {
    import MReasonTypes._
    // TODO 2.12 Организовать с помощью sealed. В scala-2.12 должны были уже починить.
    compositePickler[MReasonType]
      .addConcreteType[OnMainScreen.type]
      .addConcreteType[GeoArea.type]
      .addConcreteType[BlockModulesCount.type]
      .addConcreteType[Tag.type]
      .addConcreteType[Rcvr.type]
      .addConcreteType[GeoLocCapture.type]
  }

  @inline implicit def univEq: UnivEq[MReasonType] = UnivEq.derive

  implicit def mReasonTypeFormat: Format[MReasonType] =
    EnumeratumUtil.valueEnumEntryFormat( MReasonTypes )

}
