package util.adv

import java.time.{DayOfWeek, LocalDate}

import com.google.inject.Inject
import io.suggest.bill._
import io.suggest.bill.price.dsl._
import io.suggest.cal.m.MCalTypes
import io.suggest.dt.{MYmd, YmdHelpersJvm}
import io.suggest.primo.id.OptId
import io.suggest.util.logs.MacroLogsImpl
import models.MNode
import models.adv.{IAdvBillCtx, MAdvBillCtx}
import models.blk.{BlockHeights, BlockMeta, BlockWidths}
import models.mcal.MCalsCtx
import models.mdt.IDateStartEnd
import models.mproj.ICommonDi
import util.billing.TfDailyUtil
import util.cal.CalendarUtil

import scala.annotation.tailrec
import scala.concurrent.Future

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 19.01.17 21:57
  * Description: Какая-то очень общая утиль для размещения.
  */
class AdvUtil @Inject() (
                          tfDailyUtil             : TfDailyUtil,
                          calendarUtil            : CalendarUtil,
                          ymdHelpersJvm           : YmdHelpersJvm,
                          mCommonDi               : ICommonDi
                        )
  extends MacroLogsImpl
{

  import mCommonDi._
  import ymdHelpersJvm.Implicits.LocalDateYmdHelper

  /** Дни недели, относящиеся к выходным. Задаются списком чисел от 1 (пн) до 7 (вс), согласно DateTimeConstants. */
  private val WEEKEND_DAYS: Set[Int] = {
    import scala.collection.JavaConversions._

    configuration.getIntList("weekend.days")
      .fold[TraversableOnce[Int]] {
        Iterator(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY)
          .map(_.getValue)
      } {
        _.iterator().map(_.intValue)
      }
      .toSet
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


  /** Сборка считалки стоимости размещения на указанном ресивере.
    * Это реинкарнация метода calculateAdvPriceOnRcvr(), без кривых листенеров и соответствующего кода.
    *
    * @param tfRcvrId id ресивера.
    * @param abc Контекст рассчётов.
    * @return Для получения цены можно вызвать .price.
    */
  def calcDateAdvPriceOnTf(tfRcvrId: String, abc: IAdvBillCtx): IPriceDslTerm = {
    lazy val logPrefix = s"calcDateAdvPriceOnTf($tfRcvrId)[${System.currentTimeMillis}]:"

    // Извлечь подходящий тариф из карты тарифов узлов.
    abc.tfsMap.get(tfRcvrId).fold[IPriceDslTerm] {
      // TODO Валюта нулевого ценника берётся с потолка. Нужен более адекватный источник валюты.
      val res = MPrice(0d, MCurrencies.default)
      LOGGER.debug(s"$logPrefix Missing TF for $tfRcvrId. Guessing adv as free: $res")
      BaseTfPrice(
        price = res
      )

    } { tf =>

      LOGGER.trace(s"$logPrefix Starting with tf = $tf")

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
            calId <- clause.calId
            calCtx <- abc.mcalsCtx.calsMap.get(calId)
          } yield {
            clause -> calCtx
          }
        }
        .toSeq

      // Кешируем тут значение списка выходных, на всякий случай.
      val weekendDays = WEEKEND_DAYS

      // Рассчет стоимости для одной даты (дня) размещения.
      def calculateDateAdvPrice(day: LocalDate): IPriceDslTerm = {
        val dayOfWeek = day.getDayOfWeek.getValue

        val clause4dayOpt = clausesWithCals
          .find { case (_, calCtx) =>
            calCtx.mcal.calType.maybeWeekend(dayOfWeek, weekendDays) || calCtx.mgr.isHoliday(day)
          }

        // Пройтись по праздничным календарям, попытаться найти подходящий
        val clause4day = clause4dayOpt
          .fold(clauseDflt)(_._1)

        LOGGER.trace(s"$logPrefix $day -> ${clause4day.name} +${clause4day.amount} ${tf.currency}")

        val dayAmount = clause4day.amount

        BaseTfPrice(
          price     = MPrice(dayAmount, tf.currency),
          mCalType  = clause4dayOpt
            .map(_._2.mcal.calType)
            .orElse( Some(MCalTypes.WeekDay) ),
          date      = Some( MYmd.from(day) )
        )
      }

      // Цикл суммирования стоимости дат, начиная с $1 и заканчивая dateEnd.
      @tailrec def walkDaysAndPrice(day: LocalDate, accRev0: List[IPriceDslTerm] = Nil): List[IPriceDslTerm] = {
        val accRev1 = calculateDateAdvPrice(day) :: accRev0
        val day1 = day.plusDays(1)
        if (!day1.isBefore(dateEnd)) {
          accRev1.reverse
        } else {
          walkDaysAndPrice(day1, accRev1)
        }
      }

      // amount1 - минимальная оплата одного минимального блока по времени
      val amount1 = Sum(
        walkDaysAndPrice(dateStart)
      )

      // amountN -- amount1 домноженная на кол-во блоков карточки.
      val bmc = abc.blockModulesCount
      val amountN = Mapper(
        multiplifier = Some(bmc),
        reason       = Some(
          MPriceReason(
            reasonType  = MReasonTypes.BlockModulesCount,
            ints        = bmc :: Nil
          )
        ),
        underlying = amount1
      )

      LOGGER.trace(s"$logPrefix amount (min/full) = ${amount1.price} / ${amountN.price}")
      amountN
    }
  }


  /**
    * Рассчитать ценник размещения рекламной карточки.
    * Цена блока рассчитывается по площади, тарифам размещения узла-получателя и исходя из будней-праздников.
    *
    * @param rcvrId id ресивера, тариф которого надо использовать.
    * @param abc Контекст данных биллинга.
    * @return Стоимость размещения.
    */
  @deprecated("Use calcDateAdvPriceOnTf().price instead.", "2017.apr.10")
  def calculateAdvPriceOnRcvr(rcvrId: String, abc: IAdvBillCtx): MPrice = {
    calcDateAdvPriceOnTf(rcvrId, abc).price
  }


  /**
    * Собрать контект rcvr-биллинга для вызова calculateAdvPriceOnRcvr().
    *
    * @param mad Размещаемая рекламная карточка.
    * @param rcvrIds id ресиверов. Желательно, в виде множества без дубликатов.
    * @param ivl Период размещения.
    * @return Фьючерс с готовым к использованию контекстом rcvr-биллинга.
    */
  def rcvrBillCtx(mad: MNode, rcvrIds: TraversableOnce[String], ivl: IDateStartEnd): Future[MAdvBillCtx] = {

    // Собираем все упомянутые узлы.
    val rcvrsFut = mNodesCache.multiGet(rcvrIds)

    // Собираем карту тарифов размещения на узлах.
    val tfsMapFut = rcvrsFut.flatMap( tfDailyUtil.getNodesTfsMap )

    // Оформить собранные ресиверы в карту по id.
    val rcvrsMapFut = OptId.elsFut2idMapFut[String, MNode](rcvrsFut)

    // Получить необходимые календари, также составив карту по id
    val calsCtxFut = tfsMapFut.flatMap { tfsMap =>
      val calIds = tfDailyUtil.tfsMap2calIds( tfsMap )
      calendarUtil.getCalsCtx(calIds)
    }

    // Пока посчитать размеры карточки
    val bmc = getAdModulesCount(mad)

    for {
      tfsMap    <- tfsMapFut
      calsCtx   <- calsCtxFut
      rcvrsMap  <- rcvrsMapFut
    } yield {
      MAdvBillCtx(
        blockModulesCount = bmc,
        mcalsCtx  = calsCtx,
        tfsMap    = tfsMap,
        ivl       = ivl,
        mad       = mad,
        rcvrsMap  = rcvrsMap
      )
    }
  }


  /** Контекст для бесплатного размещения. */
  def freeRcvrBillCtx(mad: MNode, ivl: IDateStartEnd): MAdvBillCtx = {
    val bmc = getAdModulesCount(mad)
    MAdvBillCtx(
      blockModulesCount = bmc,
      mcalsCtx          = MCalsCtx.empty,
      tfsMap            = Map.empty,
      ivl               = ivl,
      mad               = mad,
      rcvrsMap          = Map.empty
    )
  }

}
