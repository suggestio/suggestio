package io.suggest.bill.price.dsl

import enumeratum.values.{StringEnum, StringEnumEntry}
import io.suggest.common.html.HtmlConstants
import io.suggest.geo.DistanceUtil
import io.suggest.i18n.{MessagesF_t, MsgCodes}
import io.suggest.mbill2.m.item.typ.MItemTypes

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 10.04.17 15:28
  * Description: Модели типов причины.
  * Причина начисления заворачивается в доп.класс, чтобы можно передавать какие-то пояснения или аргументы.
  */

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

}


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

    override def i18nPayload(payload: MPriceReason)(messagesF: MessagesF_t): Option[String] = {
      // Пытаемся отрендерить инфу по гео-кругу.
      for {
        mgc       <- payload.geoCircles.headOption
      } yield {
        val distanceStr = DistanceUtil.formatDistanceM( mgc.radiusM )(messagesF)
        val coordsStr = mgc.center.toHumanFriendlyString
        messagesF(
          MsgCodes.`in.radius.of.0.from.1`,
          distanceStr :: coordsStr :: Nil
        )
      }
    }

  }


  /** Накидывание цены за фактическую площадь карточки в плитке. */
  case object BlockModulesCount extends MReasonType("bmc") {
    override def msgCodeI18n  = MsgCodes.`Ad.area.modules.count`
    /** Кол-во блоков не является оплачиваемым, а является просто множителем для других item-причин. */
    override def isItemLevel = false

    override def i18nPayload(payload: MPriceReason)(messagesF: MessagesF_t): Option[String] = {
      for {
        bmc <- payload.ints.headOption
      } yield {
        messagesF( MsgCodes.`N.modules`, bmc :: Nil )
      }
    }

  }


  /** Накидывают за тег. */
  case object Tag extends MReasonType("tag") {
    override def isItemLevel  = true
    override def msgCodeI18n  = MsgCodes.`Tag`

    override def i18nPayload(payload: MPriceReason)(messagesF: MessagesF_t): Option[String] = {
      for {
        tagFace <- payload.strings.headOption
      } yield {
        HtmlConstants.TAG_PREFIX + tagFace
      }
    }

  }

  /** Накидывают за прямое размещение на каком-то узле-ресивере. */
  case object Rcvr extends MReasonType("rcvr") {
    /** Это не является item-уровнем. item'ами являются теги и OnMainScreen в underlying-термах. */
    override def isItemLevel = false
    override def msgCodeI18n  = MItemTypes.AdvDirect.nameI18n

    override def i18nPayload(payload: MPriceReason)(messagesF: MessagesF_t): Option[String] = {
      for {
        nameId <- payload.nameIds.headOption
      } yield {
        nameId.name
      }
    }

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

  /** Локализованный payload, если есть. */
  def i18nPayload(payload: MPriceReason)(messagesF: MessagesF_t): Option[String] = {
    None
  }

  override final def toString = value

}
