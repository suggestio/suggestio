package util.billing

import models.MBillMmpDaily
import io.suggest.ym.parsers.Price
import org.joda.time.LocalDate
import org.joda.time.DateTimeConstants._
import scala.annotation.tailrec

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 28.05.14 19:04
 * Description: Утиль для работы с биллингом, где имеют вес площади и расценки получателя рекламы.
 */
object MmpDailyBilling {

  /**
   * Рассчитать ценник размещения рекламной карточки.
   * Цена блока рассчитывается по площади, тарифам размещения узла-получателя и исходя из будней-праздников.
   * @return
   */
  def calculateAdvPrice(blockModulesCount: Int, rcvrPricing: MBillMmpDaily, dateStart: LocalDate, dateEnd: LocalDate): Price = {
    // Во избежание бесконечного цикла, огораживаем dateStart <= dateEnd
    assert(!dateStart.isAfter(dateEnd), "dateStart must not be after dateEnd")
    def calculateDateAdvPrice(day: LocalDate): Float = {
      val isWeekend = day.getDayOfWeek match {
        case (SUNDAY | SATURDAY) => true
        case _ => false
      }
      if (isWeekend) {
        rcvrPricing.mmpWeekend
      } else {
        rcvrPricing.mmpWeekday
      }
    }
    @tailrec def walkDaysAndPrice(day: LocalDate, acc: Float): Float = {
      val acc1 = calculateDateAdvPrice(day) + acc
      val day1 = day.plusDays(1)
      if (day1 isAfter dateEnd) {
        acc1
      } else {
        walkDaysAndPrice(day1, acc1)
      }
    }
    val amount1 = walkDaysAndPrice(dateStart, 0F)
    val amountAllBlocks = blockModulesCount * amount1
    Price(amountAllBlocks, rcvrPricing.currency)
  }


}
