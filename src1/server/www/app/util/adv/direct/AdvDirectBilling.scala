package util.adv.direct

import java.{time => jat}

import com.google.inject.{Inject, Singleton}
import io.suggest.bill.MPrice
import io.suggest.mbill2.m.balance.MBalances
import io.suggest.mbill2.m.gid.Gid_t
import io.suggest.mbill2.m.item.status.{MItemStatus, MItemStatuses}
import io.suggest.mbill2.m.item.typ.MItemTypes
import io.suggest.mbill2.m.item.{MItem, MItems}
import io.suggest.model.n2.bill.tariff.daily.ITfClauses
import models._
import models.adv.direct.{AdvFormEntry, FormResult}
import models.mcal.{ICalsCtx, MCalendars}
import models.mdt.IDateStartEnd
import models.mproj.ICommonDi
import org.joda.time.DateTimeConstants._
import org.joda.time.LocalDate
import util.PlayMacroLogsImpl
import util.adv.AdvUtil
import util.billing.TfDailyUtil
import util.cal.CalendarUtil

import scala.annotation.tailrec
import scala.collection.JavaConversions._
import scala.concurrent.Future

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 28.05.14 19:04
 * Description: Утиль для работы с биллингом, где имеют вес площади и расценки получателя рекламы.
 */
@Singleton
class AdvDirectBilling @Inject() (
  tfDailyUtil             : TfDailyUtil,
  advUtil                 : AdvUtil,
  calendarUtil            : CalendarUtil,
  mCalendars              : MCalendars,
  mBalances               : MBalances,
  mItems                  : MItems,
  val mCommonDi           : ICommonDi
)
  extends PlayMacroLogsImpl
{

  import LOGGER._
  import mCommonDi._
  import slick.profile.api._

  /** Дни недели, относящиеся к выходным. Задаются списком чисел от 1 (пн) до 7 (вс), согласно DateTimeConstants. */
  private val WEEKEND_DAYS: Set[Int] = {
    configuration.getIntList("weekend.days")
      .map(_.map(_.intValue).toSet)
      .getOrElse( Set(FRIDAY, SATURDAY, SUNDAY) )
  }

  /** Довольно грубый метод поиска уже занятых карточкой ресиверов.
    * Нужно переписать форму, чтобы можно было размещать в незанятые даты. */
  def findBusyRcvrsMap(adId: String): DBIOAction[Map[String, MItem], NoStream, Effect.Read] = {
    for {
      items <- sqlForAd(adId).result
    } yield {
      val iter = for {
        mitem <- items
        rcvrId <- mitem.rcvrIdOpt
      } yield {
        rcvrId -> mitem
      }
      iter.toMap
    }
  }

  private def sqlForAd(adId: String) = {
    mItems
      .query
      .filter { i =>
        (i.nodeId === adId) &&
          (i.iTypeStr === MItemTypes.AdvDirect.strId) &&
          (i.statusStr inSet MItemStatuses.advBusyIds.toSeq)
      }
  }

  /** Поиск занятых ресиверов для карточки среди запрошенных для размещения. */
  def findBusyRcvrsExact(adId: String, fr: FormResult): DBIOAction[Set[String], NoStream, Effect.Read] = {
    // Подготовить значения аргументов для вызова экшена
    val dtStart = fr.period.dateStart.toDateTimeAtStartOfDay
    val dtEnd   = fr.period.dateEnd.toDateTimeAtStartOfDay()

    // Сборка логики db-экшена
    val dbAction = sqlForAd(adId)
      .filter { i =>
        // Интересуют только ряды, которые относятся к прямому размещению текущей карточки...
        (i.rcvrIdOpt inSet fr.nodeIdsIter.toSet) && (
          // Хотя бы одна из дат (start, end) должна попадать в уже занятый период размещения.
          (i.dateStartOpt <= dtStart && i.dateEndOpt >= dtStart) ||
          (i.dateStartOpt <= dtEnd   && i.dateEndOpt >= dtEnd)
        )
      }
      .map { i =>
        i.rcvrIdOpt
      }
      .result

    // Отмаппить результат экшена прямо в экшене, т.к. он слишком некрасивый.
    for (seqOpts <- dbAction) yield {
      seqOpts.iterator
        .flatten
        .toSet
    }
  }


  /**
   * Рассчитать ценник размещения рекламной карточки.
   * Цена блока рассчитывается по площади, тарифам размещения узла-получателя и исходя из будней-праздников.
   *
   * @return
   */
  def calculateAdvPrice(blockModulesCount: Int, tf: ITfClauses, ivl: IDateStartEnd, mcalsCtx: ICalsCtx): MPrice = {
    // Инициализация логгирования
    lazy val logPrefix = s"calculateAdvPrice(${System.currentTimeMillis}):"
    trace(s"$logPrefix rcvr: square=$blockModulesCount dates=${ivl.dateStart}..${ivl.dateEnd}")

    val dateStart = ivl.dateStart
    val dateEnd = ivl.dateEnd
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
          calCtx  <- mcalsCtx.calsMap.get(calId)
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
      val dayJat = jat.LocalDate.of(day.getYear, day.getMonthOfYear, day.getDayOfMonth)
      val dayOfWeek = day.getDayOfWeek

      // Пройтись по праздничным календарям, попытаться найти подходящий
      val clause4day = clausesWithCals
        .find { case (clause, calCtx) =>
          calCtx.mcal.calType.maybeWeekend(dayOfWeek, weekendDays) || calCtx.mgr.isHoliday(dayJat)
        }
        .map(_._1)
        .getOrElse(clauseDflt)

      trace(s"$logPrefix $day -> ${clause4day.name} +${clause4day.amount} ${tf.currency}")
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
    val amountN = blockModulesCount * amount1

    trace(s"$logPrefix amount (min/full) = $amount1 / $amountN")
    MPrice(amountN, tf.currency)
  }


  /**
   * Посчитать цену размещения рекламной карточки согласно переданной спеке.
   *
   * @param mad Размещаемая рекламная карточка.
   * @param adves2 Требования по размещению карточки на узлах.
   */
  def getAdvPrices(mad: MNode, adves2: Seq[AdvFormEntry]): Future[Seq[MPrice]] = {
    val rcvrsFut = mNodeCache.multiGet {
      adves2
        .iterator
        .map(_.adnId)
        .toSet
    }

    // Карта тарифов по id узлов.
    val tfsMapFut = rcvrsFut.flatMap( tfDailyUtil.getNodesTfsMap )

    // Получить необходимые календари, также составив карту по id
    val calsCtxFut = tfsMapFut.flatMap { tfsMap =>
      val calIds = tfDailyUtil.tfsMap2calIds( tfsMap )
      calendarUtil.getCalsCtx(calIds)
    }

    // Пока посчитать размеры карточки
    val bmc = advUtil.getAdModulesCount(mad)

    // Когда всё будет готово, нужно нагенерить результатов.
    for {
      tfsMap  <- tfsMapFut
      calsCtx <- calsCtxFut
    } yield {
      // Выдать список цен из списка запрашиваемых размещений.
      adves2
        .iterator
        .map { adve =>
          val tf = tfsMap( adve.adnId )
          calculateAdvPrice(bmc, tf, adve, calsCtx)
        }
        .toSeq
      // Для суммирования списка по валютам и получения итоговой цены надо использовать MPrice.sumPricesByCurrency().
    }
  }

  /**
    * Сохранить в БД реквесты размещения рекламных карточек.
    *
    * @param orderId id ордера-корзины.
    * @param mad рекламная карточка.
    * @param advs Список запросов на размещение.
    * @param rcvrTfs Карта тарифов ресиверов, ключ -- это id узла-ресивера.
    *                См. [[util.billing.TfDailyUtil.getNodesTfsMap()]].
    * @param mcalsCtx Контекст календарей.
    * @return db-экшен, добавляющий запросы размещения в корзину.
    */
  def mkAdvReqItems(orderId: Gid_t, mad: MNode, advs: TraversableOnce[AdvFormEntry], status: MItemStatus,
                    rcvrTfs: Map[String, MDailyTf], mcalsCtx: ICalsCtx): Iterator[MItem] = {
    val bmc = advUtil.getAdModulesCount(mad)

    for (adv <- advs.toIterator if adv.advertise) yield {
      MItem(
        orderId       = orderId,
        iType         = MItemTypes.AdvDirect,
        status        = status,
        price         = calculateAdvPrice(bmc, rcvrTfs(adv.adnId), adv, mcalsCtx),
        nodeId        = mad.id.get,
        sls           = adv.showLevels,
        dateStartOpt  = Some(adv.dateStart.toDateTimeAtStartOfDay),
        dateEndOpt    = Some(adv.dateEnd.toDateTimeAtStartOfDay),
        rcvrIdOpt     = Some(adv.adnId)
      )
    }
  }

}
