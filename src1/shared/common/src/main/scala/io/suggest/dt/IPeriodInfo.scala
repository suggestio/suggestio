package io.suggest.dt

import boopickle.Default._
import io.suggest.dt.interval.{MRangeYmd, QuickAdvIsoPeriod, QuickAdvPeriod, QuickAdvPeriods}

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 20.01.17 18:45
  * Description: Модель описания временнОй стороный вопроса периода размещения.
  *
  * Куча модельной логики лежит внутри классов-реализаций место статического объекта.
  * Конечно, это плохо, но так удобнее и не слишком-то создаёт проблемы для столь простой модели...
  */

object IPeriodInfo {

  implicit val periodInfoPickler: Pickler[IPeriodInfo] = {
    implicit val qapPickler = QuickAdvPeriod.qapIsoPickler
    compositePickler[IPeriodInfo]
      .addConcreteType[MIsoPeriod]
      .addConcreteType[MCustomPeriod]
  }

  def default = MIsoPeriod()

}


/** Различные варианты модели периода. */
sealed trait IPeriodInfo {

  /** Заменить/выставить ISO-период размещения. */
  def withIsoPeriod(isoPeriod: QuickAdvIsoPeriod) = MIsoPeriod(isoPeriod = isoPeriod)

  /** Заменить/выставить кастомный период размещения. */
  def withCustomRange(cr2: MRangeYmd) = MCustomPeriod(customRange = cr2)

  /** Вернуть iso-период размещения, если возможно. */
  def isoPeriodOpt: Option[QuickAdvIsoPeriod] = None

  /** Вернуть кастомный (заданный вручную юзером) период размещения, если возможно.
    * Для iso-периодов тут будет none, т.к. они кастомными не являются. */
  def customRangeOpt: Option[MRangeYmd] = None

  /** Дата начала указанного типа, если возможно. */
  def dateStart[T](implicit ev: IYmdHelper[T]): T

  /** Дата окончания указанного типа, если есть. */
  def dateEnd[T](implicit ev: IYmdHelper[T]): T

  /** Вернуть текущий режим QuickAdvPeriod. */
  def quickAdvPeriod: QuickAdvPeriod

  /** Вернуть период размещения в форме MRangeYmd. */
  def rangeYmd[Date_t](implicit ev: IYmdHelper[Date_t]): MRangeYmd

}


/**
  * Режим quick-периода размещения.
  * @param isoPeriod Выбранный шаблон периода размещения.
  */
case class MIsoPeriod(isoPeriod: QuickAdvIsoPeriod = QuickAdvPeriods.default ) extends IPeriodInfo {

  override def rangeYmd[Date_t](implicit ev: IYmdHelper[Date_t]): MRangeYmd = {
    val start = ev.now
    // учитывая, что результат now() **может быть mutable**, сразу считаем стартовый MYmd.
    val startYmd = ev.toYmd(start)
    val end = isoPeriod.updateDate(start)
    val endYmd = ev.toYmd(end)
    MRangeYmd(
      dateStart = startYmd,
      dateEnd   = endYmd
    )
  }

  override def isoPeriodOpt = Some(isoPeriod)

  override def dateStart[T](implicit ev: IYmdHelper[T]): T = {
    ev.now
  }

  override def dateEnd[T](implicit ev: IYmdHelper[T]): T = {
    isoPeriod.updateDate(ev.now)
  }

  override def quickAdvPeriod = isoPeriod

}


/**
  * Режим произвольного периода размещения.
  * @param customRange Кастомный интервал дат.
  */
case class MCustomPeriod(customRange: MRangeYmd) extends IPeriodInfo {

  override def rangeYmd[Date_t](implicit ev: IYmdHelper[Date_t]): MRangeYmd = {
    customRange
  }

  override def customRangeOpt = Some(customRange)

  override def dateStart[T](implicit ev: IYmdHelper[T]): T = {
    customRange.dateStart.to[T]
  }

  override def dateEnd[T](implicit ev: IYmdHelper[T]): T = {
    customRange.dateEnd.to[T]
  }

  override def quickAdvPeriod = QuickAdvPeriods.Custom

}
