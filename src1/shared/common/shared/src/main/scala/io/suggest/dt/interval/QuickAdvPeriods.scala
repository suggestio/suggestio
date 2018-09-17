package io.suggest.dt.interval

import boopickle.CompositePickler
import boopickle.Default._
import enumeratum.values.{StringEnum, StringEnumEntry}
import io.suggest.dt.IYmdHelper
import io.suggest.enum2.EnumeratumUtil
import japgolly.univeq.UnivEq
import play.api.libs.json._
import play.api.libs.functional.syntax._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 20.12.16 21:24
  * Description: Пошаренная между клиентом и сервером модель "быстрых" периодов размещения.
  */
object QuickAdvPeriod {

  import QuickAdvPeriods._

  implicit val qapIsoPickler: CompositePickler[QuickAdvIsoPeriod] = {
    compositePickler[QuickAdvIsoPeriod]
      .addConcreteType[P3D.type]
      .addConcreteType[P1W.type]
      .addConcreteType[P1M.type]
  }

  implicit val qapPickler: CompositePickler[QuickAdvPeriod] = {
    // TODO Есть проблема с sealed и автогенерацией в scala-2.11: boopickle не видит реализации sealed-класса внутри модели.
    // На scala-2.12 проблема отчасти решена, т.е. можно выкрутиться с помощью generatePickler[] внутри QuickAdvPeriods.
    compositePickler[QuickAdvPeriod]
      .addConcreteType[P3D.type]
      .addConcreteType[P1W.type]
      .addConcreteType[P1M.type]
      .addConcreteType[Custom.type]
  }

  implicit def univEq: UnivEq[QuickAdvPeriod] = UnivEq.derive

  implicit def quickAdvPeriodFormat: Format[QuickAdvPeriod] = {
    EnumeratumUtil.valueEnumEntryFormat( QuickAdvPeriods )
  }

  /** Префикс кода локализованного названия периода (i18n). */
  def MESSAGES_PREFIX = "adv.period."

}

/** Класс этой enum-модели. */
sealed abstract class QuickAdvPeriod(override val value: String) extends StringEnumEntry {

  // Из-за IStrId странность происходит: case object корректно реализуют .toString(),
  // а мы переопределяем его через такое же значение из PeriodConstants.

  def quickAdvIsoPeriod: Option[QuickAdvIsoPeriod]

  def isoPeriodOpt: Option[String]

  def messagesCode: String = {
    QuickAdvPeriod.MESSAGES_PREFIX + value
  }

  def isCustom: Boolean

}


object QuickAdvIsoPeriod {

  /** Поддержка play-json для подмножества значений. */
  implicit def quickAdvIsoPeriodFormat: Format[QuickAdvIsoPeriod] = {
    val fmt0 = QuickAdvPeriod.quickAdvPeriodFormat
    val reads2 = fmt0
      .filter(_.isoPeriodOpt.nonEmpty)
      .map(_.asInstanceOf[QuickAdvIsoPeriod])
    val writes2 = (fmt0: Writes[QuickAdvPeriod])
      .contramap[QuickAdvIsoPeriod](identity)
    Format[QuickAdvIsoPeriod](reads2, writes2)
  }

  implicit def univEq: UnivEq[QuickAdvIsoPeriod] = UnivEq.derive

}

/** Реальные (не кастомные) период имеют поле isoPeriod с обязательным значением. */
sealed abstract class QuickAdvIsoPeriod(value: String) extends QuickAdvPeriod(value) {

  final def isoPeriod = value
  override def isoPeriodOpt = Some(isoPeriod)
  override def isCustom = false
  override def quickAdvIsoPeriod = Some(this)

  /** Накладывание периода на абстрактную дату.
    * Метод абстрагирован от конкретной реализации, поэтому можно заюзать как с js.Date,
    * так и с jvm8-datetime-api.
    *
    * @param date Исходная дата. Может быть модифицирована изнутри в зависимости от используемой платформы.
    * @return Обновлённая дата.
    *         Возможно, тот же самый, но модифицированный инстанс, если исходная тип даты есть mutable по своей природе (js.Date).
    */
  def updateDate[Date_t](date: Date_t)(implicit dateHelper: IYmdHelper[Date_t]): Date_t

}


/** Статическая enum-модель быстрых периодов размещения. */
object QuickAdvPeriods extends StringEnum[QuickAdvPeriod] {

  def default: QuickAdvIsoPeriod = P3D

  def withNameOptionIso(n: String): Option[QuickAdvIsoPeriod] = {
    withValueOpt(n).flatMap {
      case iso: QuickAdvIsoPeriod => Some(iso)
      case _ => None
    }
  }


  /** 3 дня. */
  case object P3D extends QuickAdvIsoPeriod( "P3D" ) {
    override def updateDate[Date_t](date: Date_t)(implicit dateHelper: IYmdHelper[Date_t]): Date_t = {
      dateHelper.plusDays(date, 3)
    }
    override def toString = super.toString
  }

  /** 1 неделя. */
  case object P1W extends QuickAdvIsoPeriod( "P1W" ) {

    override def updateDate[Date_t](date: Date_t)(implicit dateHelper: IYmdHelper[Date_t]): Date_t = {
      dateHelper.plusWeeks(date, 1)
    }
    override def toString = super.toString
  }

  /** 1 месяц. */
  case object P1M extends QuickAdvIsoPeriod( "P1M" ) {
    override def updateDate[Date_t](date: Date_t)(implicit dateHelper: IYmdHelper[Date_t]): Date_t = {
      dateHelper.plusMonths(date, 1)
    }
    override def toString = super.toString
  }

  /** Заданный вручную период размещения. */
  case object Custom extends QuickAdvPeriod( "custom" ) {
    override def isoPeriodOpt = None
    override def isCustom     = true
    override def toString     = super.toString
    override def quickAdvIsoPeriod = None
  }


  override val values = findValues

}
