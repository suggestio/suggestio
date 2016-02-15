package util.adv.direct

import java.sql.Connection
import java.{time => jat}

import com.google.inject.{Inject, Singleton}
import io.suggest.mbill2.m.balance.{MBalance => MBalance2, MBalances}
import io.suggest.mbill2.m.gid.Gid_t
import io.suggest.mbill2.m.item.status.MItemStatus
import io.suggest.mbill2.m.item.typ.MItemTypes
import io.suggest.mbill2.m.item.{MItem, MItems}
import io.suggest.model.n2.edge.{MEdgeInfo, MNodeEdges}
import models._
import models.adv.direct.AdvFormEntry
import models.adv.tpl.MAdvPricing
import models.adv.{IAdvTerms, MAdvOk, MAdvRefuse, MAdvReq}
import models.blk.{BlockHeights, BlockWidths}
import models.mbill.{MContract => MContract1, _}
import models.mcal.{ICalsCtx, MCalendars}
import models.mproj.ICommonDi
import org.joda.time.DateTimeConstants._
import org.joda.time.{DateTime, LocalDate}
import util.PlayMacroLogsImpl
import util.async.AsyncUtil
import util.billing.TfDailyUtil
import util.cal.CalendarUtil
import util.n2u.N2NodesUtil

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
class AdvDirectBilling @Inject()(
  n2NodesUtil             : N2NodesUtil,
  tfDailyUtil             : TfDailyUtil,
  calendarUtil            : CalendarUtil,
  mCalendars              : MCalendars,
  mBalances               : MBalances,
  mItems                  : MItems,
  mCommonDi               : ICommonDi
)
  extends PlayMacroLogsImpl
{

  import LOGGER._
  import mCommonDi._

  /** Дни недели, относящиеся к выходным. Задаются списком чисел от 1 (пн) до 7 (вс), согласно DateTimeConstants. */
  private val WEEKEND_DAYS: Set[Int] = {
    configuration.getIntList("mmp.daily.weekend.days")
      .map(_.map(_.intValue).toSet)
      .getOrElse( Set(FRIDAY, SATURDAY, SUNDAY) )
  }

  /**
   * Рассчитать ценник размещения рекламной карточки.
   * Цена блока рассчитывается по площади, тарифам размещения узла-получателя и исходя из будней-праздников.
   *
   * @return
   */
  def calculateAdvPrice(blockModulesCount: Int, tf: MDailyTf, advTerms: IAdvTerms, mcalsCtx: ICalsCtx): MPrice = {
    // Инициализация логгирования
    lazy val logPrefix = s"calculateAdvPrice(${System.currentTimeMillis}):"
    trace(s"$logPrefix rcvr: square=$blockModulesCount from=${advTerms.dateStart} to=${advTerms.dateEnd} sls=${advTerms.showLevels}")

    val dateStart = advTerms.dateStart
    val dateEnd = advTerms.dateEnd
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

      trace(s"$logPrefix $day -> ${clause4day.name} +${clause4day.amount} ${tf.currencyCode}")
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
   * Высокоуровневый рассчет цены размещения рекламной карточки. Вычисляет кол-во рекламных модулей и дергает
   * другой одноимённый метод.
   *
   * @param mad Рекламная карточка или иная реализация блочного документа.
   * @return Площадь карточки.
   */
  def getAdModulesCount(mad: MNode): Int = {
    val bm = mad.ad.blockMeta.get   // TODO Следует ли отрабатывать ситуацию, когда нет BlockMeta?
    // Мультипликатор по ширине
    val wmul = BlockWidths(bm.width).relSz
    // Мультипликатор по высоте
    val hmul = BlockHeights(bm.height).relSz
    val blockModulesCount: Int = wmul * hmul
    trace(
      s"getAdModulesCount(${mad.id.getOrElse("?")}): blockModulesCount = $wmul * $hmul = $blockModulesCount ;; blockId = ${bm.blockId}"
    )
    blockModulesCount
  }


  private def assertAdvsReqRowsDeleted(rowsDeleted: Int, mustBe: Int, advReqId: Int) {
    if (rowsDeleted != mustBe)
      throw new IllegalStateException(s"Adv request $advReqId not deleted. Rows deleted = $rowsDeleted, but 1 expected.")
  }


  /**
   * Посчитать цену размещения рекламной карточки согласно переданной спеке.
   *
   * @param mad Размещаемая рекламная карточка.
   * @param adves2 Требования по размещению карточки на узлах.
   */
  def getAdvPrices(mad: MNode, adves2: List[AdvFormEntry]): Future[Seq[MPrice]] = {
    // 2016 Перепись на новую архитектуру биллинга v2. И производительность серьезно ускорена.
    val rcvrsFut = mNodeCache.multiGet {
      adves2.iterator
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
    val bmc = getAdModulesCount(mad)

    // Когда всё будет готово, нужно нагенерить результатов.
    for {
      tfsMap  <- tfsMapFut
      calsCtx <- calsCtxFut
    } yield {
      // Выдать список цен из списка запрашиваемых размещений.
      adves2.iterator
        .map { adve =>
          val tf = tfsMap( adve.adnId )
          calculateAdvPrice(bmc, tf, adve, calsCtx)
        }
        .toSeq
      // Для суммирования списка по валютам и получения итоговой цены надо использовать MPrice.sumPricesByCurrency().
    }
  }

  def getAdvPricing(prices: Seq[MPrice], balances: Seq[MBalance2]): MAdvPricing = {
    // Если есть разные валюты, то операция уже невозможна.
    // TODO Наверное, этот параметр надо будет удалить, т.к. будет отправка юзера на оплату в ПС.
    val hasEnoughtMoney = true
    /*prices.size <= 1 && {
      prices.headOption.exists { price =>
        price.currency.getCurrencyCode == mbb.currencyCode && price.amount <= mbb.amount.toDouble
      }
    }*/
    MAdvPricing(prices, hasEnoughtMoney)
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
    *
    * @return db-экшен, добавляющий запросы размещения в корзину.
    */
  def mkAdvReqItems(orderId: Gid_t, mad: MNode, advs: TraversableOnce[AdvFormEntry], status: MItemStatus,
                    rcvrTfs: Map[String, MDailyTf], mcalsCtx: ICalsCtx): Iterator[MItem] = {
    val producerId = n2NodesUtil.madProducerId(mad).get
    val bmc = getAdModulesCount(mad)

    for (adv <- advs.toIterator if adv.advertise) yield {
      MItem(
        orderId       = orderId,
        iType         = MItemTypes.AdvDirect,
        status        = status,
        price         = calculateAdvPrice(bmc, rcvrTfs(adv.adnId), adv, mcalsCtx),
        adId          = mad.id.get,
        prodId        = producerId,
        dtIntervalOpt = Some(adv.dtInterval),
        rcvrIdOpt     = Some(adv.adnId)
      )
    }
  }

  def date2DtAtEndOfDay(ld: LocalDate) = ld.toDateTimeAtStartOfDay.plusDays(1).minusSeconds(1)


  /**
   * Провести MAdvReq в MAdvOk, списав и зачислив все необходимые деньги.
   *
   * @param advReq Одобряемый реквест размещения.
   * @param isAuto Пользователь одобряет или система?
   * @return Сохранённый экземпляр MAdvOk.
   */
  def acceptAdvReq(advReq: MAdvReq, isAuto: Boolean): MAdvOk = {
    // TODO Переписать на billing v2
    // Надо провести платёж, запилить транзакции для prod и rcvr и т.д.
    val advReqId = advReq.id.get
    lazy val logPrefix = s"acceptAdvReq($advReqId): "
    trace(s"${logPrefix}Starting. isAuto=$isAuto advReq=$advReq")
    db.withTransaction { implicit c =>
      // Удалить исходный реквест размещения
      val reqsDeleted = advReq.delete
      assertAdvsReqRowsDeleted(reqsDeleted, 1, advReqId)
      // Провести все денежные операции.
      val prodAdnId = advReq.prodAdnId
      val prodContractOpt = MContract1.findForAdn(prodAdnId, isActive = Some(true)).headOption
      assert(prodContractOpt.exists(_.id.contains(advReq.prodContractId)), "Producer contract not found or changed since request creation.")
      val prodContract = prodContractOpt.get
      val amount0 = advReq.amount

      // Списать заблокированную сумму:
      val oldProdMbbOpt = MBalance.getByAdnId(prodAdnId)
      assert(oldProdMbbOpt.exists(_.currencyCode == advReq.currencyCode), "producer balance currency does not match to adv request")
      val prodMbbUpdated = MBalance.updateBlocked(prodAdnId, -amount0)
      assert(prodMbbUpdated == 1, "Failed to debit blocked amount for producer " + prodAdnId)

      val now = DateTime.now
      // Запилить единственную транзакцию списания для продьюсера
      val prodTxn = MTxn(
        contractId      = prodContract.id.get,
        amount          = -amount0,
        datePaid        = advReq.dateCreated,
        txnUid          = s"${prodContract.id.get}-$advReqId",
        paymentComment  = "Debit for advertise",
        dateProcessed   = now,
        currencyCodeOpt = Option(advReq.currencyCode)
      ).save
      trace(s"${logPrefix}Debited producer[$prodAdnId] for ${prodTxn.amount} ${prodTxn.currencyCode}. txnId=${prodTxn.id.get} contractId=${prodContract.id.get}")

      // Нужно провести транзакции, разделив уровни отображения по используемому sink'у.
      val advSsl = advReq.showLevels.groupBy(_.adnSink)
      // Все sink'и имеют одинаковый тариф. И в рамках одного реквеста одинаковый набор sl для каждого sink'а.
      // Чтобы не пересчитывать цену, можно просто поделить ей на кол-во используемых sink'ов
      assert(advSsl.valuesIterator.map(_.map(_.sl)).toSet.size == 1, "Different sls for different sinks not yet implemented.")
      // Раньше тут было начисление sc sink comission
      val rcvrTxns = advSsl
        .foldLeft( List.empty[MTxn] ) { case (acc, (sink, sinkShowLevels)) =>
          acc
        }
      // Сохранить подтверждённое размещение с инфой о платежах.
      MAdvOk(
        advReq,
        dateStatus1 = now,
        prodTxnId   = prodTxn.id,
        rcvrTxnIds  = rcvrTxns.flatMap(_.id),
        isOnline    = false,
        isPartner   = false,
        isAuto      = isAuto
      ).save
    }
  }


  /**
   * Процессинг отказа в запросе размещения рекламной карточки.
   *
   * @param advReq Запрос размещения.
   * @return Экземпляр сохранённого MAdvRefuse.
   */
  def refuseAdvReqTxn(advReq: MAdvReq, advRefused: MAdvRefuse)(implicit c: Connection): Unit = {
    val rowsDeleted = advReq.delete
    assertAdvsReqRowsDeleted(rowsDeleted, 1, advReq.id.get)
    val advr = advRefused.save
    // Разблокировать средства на счёте.
    // TODO Нужно ли округлять, чтобы blocked в минус не уходило из-за неточностей float/double?
    MBalance.blockAmount(advr.prodAdnId, -advr.amount)
  }


  /**
   * Пересчёт текущих размещений указанной рекламной карточки на основе данных MAdvOk online.
   *
   * @param adId id рекламной карточки.
   * @return Карта ресиверов.
   */
  def calcualteReceiversMapForAd(adId: String): Future[Receivers_t] = {
    // Прочесть из БД текущие размещения карточки.
    val advsOkFut = Future {
      db.withConnection { implicit c =>
        MAdvOk.findOnlineFor(adId, isOnline = true)
      }
    }(AsyncUtil.jdbcExecutionContext)
    // Привести найденные размещения карточки к карте ресиверов.
    for (advsOk <- advsOkFut) yield {
      val eIter = advsOk
        .map { advOk =>
          MEdge(
            predicate = MPredicates.Receiver,
            nodeId    = advOk.rcvrAdnId,
            info      = MEdgeInfo(sls = advOk.showLevels)
          )
        }
        // Размещение одной карточки на одном узле на разных уровнях может быть проведено через разные размещения.
        .groupBy(_.nodeId)
        .valuesIterator
        .map {
          _.reduceLeft[MEdge] { (prev, next) =>
            next.copy(
              info = next.info.copy(
                sls = next.info.sls ++ prev.info.sls
              )
            )
          }
        }
      MNodeEdges.edgesToMap1( eIter )
    }
  }

}
