package io.suggest.dt.interval

import boopickle.CompositePickler
import enumeratum._
import io.suggest.primo.IStrId
import boopickle.Default._
import io.suggest.dt.IYmdHelper

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

}

/** Класс этой enum-модели. */
sealed abstract class QuickAdvPeriod extends EnumEntry with IStrId {

  // Из-за IStrId странность происходит: case object корректно реализуют .toString(),
  // а мы переопределяем его через такое же значение из PeriodConstants.

  def isoPeriodOpt: Option[String]

  def messagesCode: String = {
    PeriodsConstants.MESSAGES_PREFIX + strId
  }

  def isCustom: Boolean

}

/** Реальные (не кастомные) период имеют поле isoPeriod с обязательным значением. */
sealed abstract class QuickAdvIsoPeriod extends QuickAdvPeriod {

  def isoPeriod = strId
  override def isoPeriodOpt = Some(isoPeriod)
  override def isCustom = false

  /** Накладывание периода на абстрактную дату.
    * Метод абстрагирован от конкретной реализации, поэтому можно заюзать как с js.Date,
    * так и с jvm8-datetime-api.
    * @param date Исходная дата. Может быть модифицирована изнутри в зависимости от используемой платформы.
    * @return Обновлённая дата.
    *         Возможно, тот же самый, но модифицированный инстанс, если исходная тип даты есть mutable по своей природе (js.Date).
    */
  def updateDate[Date_t](date: Date_t)(implicit dateHelper: IYmdHelper[Date_t]): Date_t

}


/** Статическая enum-модель быстрых периодов размещения. */
object QuickAdvPeriods extends Enum[QuickAdvPeriod] {

  override val values = findValues

  // TODO scala-2.12: Можно автоматом сгенерить всё необходимое с помощью вот этого: (вместо мануального перечисления выше по коду). Но там кажется внутри будет кое-какой лишний мусор (не важно).
  // val pickler = generatePickler[QuickAdvPeriod]

  def default: QuickAdvIsoPeriod = P3D

  def withNameOptionIso(n: String): Option[QuickAdvIsoPeriod] = {
    withNameOption(n).flatMap {
      case iso: QuickAdvIsoPeriod => Some(iso)
      case _ => None
    }
  }

  /** 3 дня. */
  case object P3D extends QuickAdvIsoPeriod {
    override def strId = PeriodsConstants.P_3DAYS

    override def updateDate[Date_t](date: Date_t)(implicit dateHelper: IYmdHelper[Date_t]): Date_t = {
      dateHelper.plusDays(date, 3)
    }
    override def toString = super.toString
  }

  /** 1 неделя. */
  case object P1W extends QuickAdvIsoPeriod {
    override def strId = PeriodsConstants.P_1WEEK

    override def updateDate[Date_t](date: Date_t)(implicit dateHelper: IYmdHelper[Date_t]): Date_t = {
      dateHelper.plusWeeks(date, 1)
    }
    override def toString = super.toString
  }

  /** 1 месяц. */
  case object P1M extends QuickAdvIsoPeriod {
    override def strId = PeriodsConstants.P_1MONTH

    override def updateDate[Date_t](date: Date_t)(implicit dateHelper: IYmdHelper[Date_t]): Date_t = {
      dateHelper.plusMonths(date, 1)
    }
    override def toString = super.toString
  }

  /** Заданный вручную период размещения. */
  case object Custom extends QuickAdvPeriod {
    override def strId        = PeriodsConstants.CUSTOM
    override def isoPeriodOpt = None
    override def isCustom     = true
    override def toString = super.toString
  }

}
