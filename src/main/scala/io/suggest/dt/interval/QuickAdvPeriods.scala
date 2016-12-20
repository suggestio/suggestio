package io.suggest.dt.interval

import boopickle.CompositePickler
import enumeratum._
import io.suggest.primo.IStrId

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 20.12.16 21:24
  * Description: Пошаренная между клиентом и сервером модель "быстрых" периодов размещения.
  */
object QuickAdvPeriod {

  import boopickle.Default._
  import QuickAdvPeriods._

  implicit val pickler: CompositePickler[QuickAdvPeriod] = {
    // TODO Есть проблема с sealed: boopickle не видит реализации sealed-класса внутри модели.
    compositePickler[QuickAdvPeriod]
      .addConcreteType[P3D.type]
      .addConcreteType[P1W.type]
      .addConcreteType[P1M.type]
      .addConcreteType[Custom.type]
  }

}

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
  def isCustom = false
}


object QuickAdvPeriods extends Enum[QuickAdvPeriod] {

  override val values = findValues

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
  }

  /** 1 неделя. */
  case object P1W extends QuickAdvIsoPeriod {
    override def strId = PeriodsConstants.P_1WEEK
  }

  /** 1 месяц. */
  case object P1M extends QuickAdvIsoPeriod {
    override def strId = PeriodsConstants.P_1MONTH
  }

  /** Заданный вручную период размещения. */
  case object Custom extends QuickAdvPeriod {
    override def strId        = PeriodsConstants.CUSTOM
    override def isoPeriodOpt = None
    override def isCustom     = true
  }

}
