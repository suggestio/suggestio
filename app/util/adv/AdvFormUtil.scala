package util.adv

import java.util.Currency

import com.google.inject.Singleton
import io.suggest.mbill2.m.price.MPrice
import models.adv.tpl.MAdvPricing
import models.{CurrencyCodeOpt, AdShowLevel, AdShowLevels}
import models.adv.form.{DatePeriodOpt_t, QuickAdvPeriod, DatePeriod_t, QuickAdvPeriods}
import org.joda.time.{LocalDate, Period}
import org.joda.time.format.ISOPeriodFormat
import play.api.data.Form
import play.api.data._, Forms._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 03.12.15 20:39
 * Description: Общая утиль для маппингов разных форм размещения рекламной карточки.
 */
@Singleton
class AdvFormUtil {

  /** Значение поля node[].period.period в случае, когда юзер хочет вручную задать даты начала и окончания. */
  def CUSTOM_PERIOD = "custom"

  /** Отдельный маппинг для adv-формы, который парсит исходные данные по бесплатному размещению. */
  def freeAdvFormM: Form[Option[Boolean]] = {
    Form(
      "freeAdv" -> optional(boolean)
    )
  }

  /** Генератор списка шаблонных периодов размещения. */
  def advPeriodsAvailable = {
    val isoPeriodsIter = QuickAdvPeriods.ordered
      .iterator
      .map(_.isoPeriod)
    val iter = isoPeriodsIter ++ Iterator(CUSTOM_PERIOD)
    iter.toSeq
  }


  /** Маппинг для вертикальных уровней отображения. */
  def adSlsM: Mapping[Set[AdShowLevel]] = {
    val b = boolean
    mapping(
      "onStartPage" -> b,
      "onRcvrCat"   -> b
    )
    {(onStartPage, onRcvrCat) =>
      var acc = List[AdShowLevel]( AdShowLevels.LVL_PRODUCER )
      if (onStartPage)
        acc ::= AdShowLevels.LVL_START_PAGE
      if (onRcvrCat)
        acc ::= AdShowLevels.LVL_CATS
      acc.toSet
    }
    {adSls =>
      val onStartPage = adSls contains AdShowLevels.LVL_START_PAGE
      val onRcvrCat = adSls contains AdShowLevels.LVL_CATS
      Some((onStartPage, onRcvrCat))
    }
  }


  /** Маппинг для интервала дат размещения. Его точно нельзя заворачивать в val из-за LocalDate.now(). */
  def advDatePeriodOptM: Mapping[DatePeriodOpt_t] = {
    // option используется, чтобы избежать ошибок маппинга, если галочка isAdv убрана для текущего ресивера, и дата не выставлена одновременно.
    // TODO Неправильно введённые даты надо заворачивать в None.
    val dateOptM = optional( jodaLocalDate("yyyy-MM-dd") )
    tuple(
      "start" -> dateOptM
        .verifying("error.date.start.before.today", {dOpt => dOpt match {
          case Some(d)  => !d.isBefore(LocalDate.now)
          case None     => true
        }}),
      "end"   -> dateOptM
    )
    .transform [Option[(LocalDate, LocalDate)]] (
      {case (Some(dateStart), Some(dateEnd))  =>  Some(dateStart -> dateEnd)
       case _  =>  None },
      {case Some((dateStart, dateEnd))  =>  Some(dateStart) -> Some(dateEnd)
       case None  =>  None -> None }
    )
  }

  /** Форма исповедует select, который имеет набор предустановленных интервалов, а также имеет режим задания дат вручную. */
  def advPeriodM: Mapping[DatePeriod_t] = {
    val custom = CUSTOM_PERIOD
    tuple(
      "period" -> nonEmptyText(minLength = 1, maxLength = 10)
        .transform [Option[QuickAdvPeriod]] (
          {periodRaw =>
            if (periodRaw == custom)
              None
            else
              QuickAdvPeriods.maybeWithName(periodRaw)
          },
          { _.fold(custom)(_.isoPeriod) }
        ),
      "date"  -> advDatePeriodOptM
    )
    .verifying("error.required", { m => m match {
      case (periodOpt, datesOpt)  =>  periodOpt.isDefined || datesOpt.isDefined
    }})
      // Проверяем даты у тех, у кого выставлены галочки. end должна быть не позднее start.
    .verifying("error.date.end.before.start", { m => m match {
       // Если даты имеют смысл, то они заданы, и их проверяем.
       case (None, Some((dateStart, dateEnd)))    => !(dateStart isAfter dateEnd)
       // Остальные случаи не отрабатываем - смысла нет.
       case _ => true
    }})

    .transform [DatePeriod_t] (
      // В зависимости от имеющихся значений полей выбираем реальный период.
      { case (Some(qap), _) =>
          val now = LocalDate.now()
          now -> now.plus( qap.toPeriod.minusDays(1) )
        case (_, dpo) =>
          dpo.get
      },
      // unapply(). Нужно попытаться притянуть имеющийся интервал дат на какой-то период из списка QuickAdvPeriod.
      // При неудаче вернуть кастомный период.
      {case dp @ (dateStart, dateEnd) =>
        // Угадываем период либо откатываемся на custom_period
        val periodStr = new Period(dateStart, dateEnd).toString(ISOPeriodFormat.standard())
        QuickAdvPeriods.maybeWithName(periodStr) match {
          case Some(qap)  =>  Some(qap) -> None
          case None       =>  None -> Some(dp)
        }
      }
    )
  }

}
