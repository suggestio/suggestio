package util.adv

import com.google.inject.Inject
import io.suggest.bill.MPrice
import io.suggest.model.n2.bill.tariff.daily.ITfClauses
import models.MNode
import models.adv.MAdvBillCtx
import models.blk.{BlockHeights, BlockMeta, BlockWidths}
import models.mproj.ICommonDi
import org.joda.time.LocalDate
import util.PlayMacroLogsImpl
import util.billing.TfDailyUtil

import scala.annotation.tailrec

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 19.01.17 21:57
  * Description: Какая-то очень общая утиль для размещения.
  */
class AdvUtil @Inject() (
                          tfDailyUtil             : TfDailyUtil,
                          mCommonDi               : ICommonDi
                        )
  extends PlayMacroLogsImpl
{

  import mCommonDi._

  /** Дни недели, относящиеся к выходным. Задаются списком чисел от 1 (пн) до 7 (вс), согласно DateTimeConstants. */
  private val WEEKEND_DAYS: Set[Int] = {
    import org.joda.time.DateTimeConstants._
    import scala.collection.JavaConversions._

    configuration.getIntList("weekend.days")
      .map(_.map(_.intValue).toSet)
      .getOrElse( Set(FRIDAY, SATURDAY, SUNDAY) )
  }


  /**
    * Высокоуровневый рассчет цены размещения рекламной карточки. Вычисляет кол-во рекламных модулей и дергает
    * другой одноимённый метод.
    *
    * @param mad Рекламная карточка или иная реализация блочного документа.
    * @return Площадь карточки.
    *         NoSuchElementException, если узел не является рекламной карточкой.
    */
  def getAdModulesCount(mad: MNode): Int = {
    val bm = mad.ad.blockMeta.get   // TODO Следует ли отрабатывать ситуацию, когда нет BlockMeta?
    getAdModulesCount(bm)
  }
  def getAdModulesCount(bm: BlockMeta): Int = {
    // Мультипликатор по ширине
    val wmul = BlockWidths(bm.width).relSz
    // Мультипликатор по высоте
    val hmul = BlockHeights(bm.height).relSz
    wmul * hmul
  }




  /**
   * Рассчитать ценник размещения рекламной карточки.
   * Цена блока рассчитывается по площади, тарифам размещения узла-получателя и исходя из будней-праздников.
   *
   * @return
   */
  // TODO Переписать с joda-time окончательно на jsr310.
  def calculateAdvPrice(tf: ITfClauses, abc: MAdvBillCtx): MPrice = {
    // Инициализация логгирования
    lazy val logPrefix = s"calculateAdvPrice(${System.currentTimeMillis}):"
    LOGGER.trace(s"$logPrefix abc = $abc")

    val dateStart = abc.ivl.dateStart
    val dateEnd = abc.ivl.dateEnd
    // Проверять dateStart <= dateEnd не требуется, т.к. цикл суммирования проверяет это на каждом шаге.
    //assert(!(dateStart isAfter dateEnd), "dateStart must not be after dateEnd")

    // Разбиваем правила tf.clauses на дефолтовое и остальные, привязанные к календарям.
    // По будним (~некалендарным) дням используется это правило:
    val clauseDflt = tf.clauses
      .valuesIterator
      .find(_.calId.isEmpty)
      .getOrElse {
        // Should not happen: посуточный тариф без дефолтового правила
        LOGGER.error(s"$logPrefix WeekDay clause is undefined for $tf. This is a configuration error in rcvr-node.")
        tfDailyUtil.VERY_DEFAULT_WEEKDAY_CLAUSE
      }

    // Собрать правила с календарями для остальных дней. Правил календарных может и не быть вообще.
    val clausesWithCals = tf.clauses
      .valuesIterator
      .flatMap { clause =>
        for {
          calId   <- clause.calId
          calCtx  <- abc.mcalsCtx.calsMap.get(calId)
        } yield {
          clause -> calCtx
        }
      }
      .toSeq

    // Кешируем тут значение списка выходных, на всякий случай.
    val weekendDays = WEEKEND_DAYS

    // Рассчет стоимости для одной даты (дня) размещения.
    def calculateDateAdvPrice(day: LocalDate): Double = {
      // jollyday работает с java 8 time, а у нас пока joda-time. Конвертим руками:
      val dayJat = java.time.LocalDate.of(day.getYear, day.getMonthOfYear, day.getDayOfMonth)
      val dayOfWeek = day.getDayOfWeek

      // Пройтись по праздничным календарям, попытаться найти подходящий
      val clause4day = clausesWithCals
        .find { case (clause, calCtx) =>
          calCtx.mcal.calType.maybeWeekend(dayOfWeek, weekendDays) || calCtx.mgr.isHoliday(dayJat)
        }
        .map(_._1)
        .getOrElse(clauseDflt)

      LOGGER.trace(s"$logPrefix $day -> ${clause4day.name} +${clause4day.amount} ${tf.currency}")
      clause4day.amount
    }

    // Цикл суммирования стоимости дат, начиная с $1 и заканчивая dateEnd.
    @tailrec def walkDaysAndPrice(day: LocalDate, acc: Double): Double = {
      val acc1 = calculateDateAdvPrice(day) + acc
      val day1 = day.plusDays(1)
      if (day1.isAfter(dateEnd)) {
        acc1
      } else {
        walkDaysAndPrice(day1, acc1)
      }
    }

    // amount1 - минимальная оплата одного минимального блока по времени
    val amount1 = walkDaysAndPrice(dateStart, 0.0)
    // amountN -- amount1 домноженная на кол-во блоков.
    val amountN = abc.blockModulesCount * amount1

    LOGGER.trace(s"$logPrefix amount (min/full) = $amount1 / $amountN")
    MPrice(amountN, tf.currency)
  }


}
