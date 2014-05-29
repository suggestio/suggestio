package util.billing

import models.{BlockConf, MAdvI, MAdT, MBillMmpDaily}
import io.suggest.ym.parsers.Price
import org.joda.time.LocalDate
import org.joda.time.DateTimeConstants._
import scala.annotation.tailrec
import util.blocks.{BfHeight, BlocksUtil, BlocksConf}
import util.PlayMacroLogsImpl

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 28.05.14 19:04
 * Description: Утиль для работы с биллингом, где имеют вес площади и расценки получателя рекламы.
 */
object MmpDailyBilling extends PlayMacroLogsImpl {

  import LOGGER._

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


  /**
   * Высокоуровневый рассчет цены размещения рекламной карточки. Вычисляет кол-во рекламных модулей и дергает
   * другой одноимённый метод.
   * @param mad Рекламная карточка.
   * @param rcvrPricing Ценовой план получателя.
   * @return Стоимость размещения в валюте получателя.
   */
  def calculateAdvPrice(mad: MAdT, rcvrPricing: MBillMmpDaily, dateStart: LocalDate, dateEnd: LocalDate): Price = {
    lazy val logPrefix = s"calculateAdvPrice(${mad.id.getOrElse("?")}): "
    val block: BlockConf = BlocksConf(mad.blockMeta.blockId)
    // Мультипликатор по ширине
    val wmul = block.blockWidth match {
      case BlocksUtil.BLOCK_WIDTH_NORMAL_PX => 2
      case BlocksUtil.BLOCK_WIDTH_NARROW_PX => 1
      case other =>
        warn(logPrefix + "Unexpected block width: " + other)
        1
    }
    // Мультипликатор по высоте
    val hmul = mad.blockMeta.height match {
      case BfHeight.HEIGHT_300 => 1
      case BfHeight.HEIGHT_460 => 2
      case BfHeight.HEIGHT_620 => 3
      case other =>
        warn(logPrefix + "Unexpected block height: " + other)
        1
    }
    val blockModulesCount: Int = wmul * hmul
    calculateAdvPrice(blockModulesCount, rcvrPricing, dateStart, dateEnd)
  }

}
