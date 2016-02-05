package util.billing

import java.{time => jat}

import com.google.inject.{Inject, Singleton}
import de.jollyday.parameter.UrlManagerParameter
import io.suggest.common.fut.FutureUtil
import io.suggest.model.n2.edge.{MNodeEdges, MEdgeInfo}
import io.suggest.model.sc.common.AdShowLevels
import models._
import models.adv.direct.AdvFormEntry
import models.adv.{MAdvReq, MAdvRefuse, MAdvOk, IAdvTerms}
import models.adv.tpl.MAdvPricing
import models.blk.{BlockWidths, BlockHeights}
import models.mbill.{MContract => MContract1}
import models.mbill._
import models.mproj.ICommonDi
import org.joda.time.{DateTime, LocalDate}
import org.joda.time.DateTimeConstants._
import util.async.AsyncUtil
import util.n2u.N2NodesUtil
import scala.annotation.tailrec
import util.PlayMacroLogsImpl
import io.suggest.ym.parsers.Price
import java.sql.Connection
import de.jollyday.HolidayManager
import java.net.URL
import controllers.routes
import scala.collection.JavaConversions._
import scala.concurrent.Future

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 28.05.14 19:04
 * Description: Утиль для работы с биллингом, где имеют вес площади и расценки получателя рекламы.
 */
@Singleton
class MmpDailyBilling @Inject() (
  n2NodesUtil             : N2NodesUtil,
  mCommonDi               : ICommonDi
)
  extends PlayMacroLogsImpl
{

  import LOGGER._
  import mCommonDi._

  val MYSELF_URL_PREFIX: String = configuration.getString("mmp.daily.localhost.url.prefix") getOrElse {
    val myPort = Option(System.getProperty("http.port")).fold(9000)(_.toInt)
    s"http://localhost:$myPort"
  }

  /** Если внезапно необходимо создать контракт, то сделать его с таким суффиксом. */
  private def CONTRACT_SUFFIX_DFLT = MContract.CONTRACT_SUFFIX_DFLT

  /** Дни недели, относящиеся к выходным. Задаются списком чисел от 1 (пн) до 7 (вс), согласно DateTimeConstants. */
  val WEEKEND_DAYS: Set[Int] = configuration.getIntList("mmp.daily.weekend.days").map(_.map(_.intValue).toSet) getOrElse Set(FRIDAY, SATURDAY, SUNDAY)



  /** Сгенерить localhost-ссылку на xml-текст календаря. */
  // Часть блокировок подавляет кеш на стороне jollyday.
  private def getUrlCalAsync(calId: String): Future[HolidayManager] = {
    // 2015.dec.25: Усилены асинхронность и кеширование, т.к. под высоко-параллельной тут возникал deadlock,
    // а jollyday-кеш (v0.4.x) это не ловил это, а блокировал всё.
    import scala.concurrent.duration._
    cacheApiUtil.getOrElseFut(calId + ".holyman", expiration = 2.minute) {
      val calUrl = new URL(MYSELF_URL_PREFIX + routes.SysCalendar.getCalendarXml(calId))
      val args = new UrlManagerParameter(calUrl, null)
      Future {
        HolidayManager.getInstance(args)
      }(AsyncUtil.singleThreadCpuContext)
    }
  }

  /**
   * Рассчитать ценник размещения рекламной карточки.
   * Цена блока рассчитывается по площади, тарифам размещения узла-получателя и исходя из будней-праздников.
   * @return
   */
  def calculateAdvPrice(blockModulesCount: Int, rcvrPricing: MTariffDaily, advTerms: IAdvTerms, mcalCtx: MCals1Ctx): Price = {
    lazy val logPrefix = s"calculateAdvPrice($blockModulesCount/${rcvrPricing.id.get}): "
    trace(s"${logPrefix}rcvr: tariffId=${rcvrPricing.id.get} mbcId=${rcvrPricing.contractId};; terms: from=${advTerms.dateStart} to=${advTerms.dateEnd} sls=${advTerms.showLevels}")
    // Во избежание бесконечного цикла, огораживаем dateStart <= dateEnd
    val dateStart = advTerms.dateStart
    val dateEnd = advTerms.dateEnd
    //assert(!(dateStart isAfter dateEnd), "dateStart must not be after dateEnd")

    def calculateDateAdvPrice(day: LocalDate): Float = {
      val dayJat = jat.LocalDate.of(day.getYear, day.getMonthOfYear, day.getDayOfMonth)

      if ( mcalCtx.prime.isHoliday(dayJat) ) {
        trace(s"$logPrefix $day -> primetime -> +${rcvrPricing.mmpPrimetime}")
        rcvrPricing.mmpPrimetime
      } else {
        val isWeekend = (WEEKEND_DAYS contains day.getDayOfWeek) || mcalCtx.weekend.isHoliday(dayJat)
        if (isWeekend) {
          trace(s"$logPrefix $day -> weekend -> +${rcvrPricing.mmpWeekend}")
          rcvrPricing.mmpWeekend
        } else {
          trace(s"$logPrefix $day -> weekday -> +${rcvrPricing.mmpWeekday}")
          rcvrPricing.mmpWeekday
        }
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
    // amount1 - минимальная оплата одного минимального блока по времени
    val amount1 = walkDaysAndPrice(dateStart, 0F)
    // amountN -- amount1 домноженная на кол-во блоков.
    val amountN: Float = blockModulesCount * amount1
    val amountTotal: Float = advTerms.showLevels.foldLeft(0F) { (amountAcc, ssl) =>
      import AdShowLevels._
      val incr = ssl.sl match {
        case LVL_CATS =>
          rcvrPricing.onRcvrCat * amountN
        case LVL_START_PAGE =>
          rcvrPricing.onStartPage * amountN
        case LVL_PRODUCER =>
          // 2015.aug.14: Решено, что не надо накидывать за каталог. Но выпливать из формы некогда, поэтому тут пока костыль.
          // showLevels set точно содержит LVL_PRODUCER. Если содержит ещё какие-то уровни, то LVL_PRODUCER НЕ тарифицировать.
          if (advTerms.showLevels.size > 1)  0F  else  amountN
      }
      val amountAcc1 = amountAcc + incr
      trace(s"$logPrefix +${ssl.sl} (sink=${ssl.adnSink.longName}): +x$incr: $amountAcc => $amountAcc1")
      amountAcc1
    }
    trace(s"$logPrefix amount (min/Block/Full) = $amount1 / $amountN / $amountTotal")
    Price(amountTotal, rcvrPricing.currency)
  }


  /**
   * Высокоуровневый рассчет цены размещения рекламной карточки. Вычисляет кол-во рекламных модулей и дергает
   * другой одноимённый метод.
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


  def assertAdvsReqRowsDeleted(rowsDeleted: Int, mustBe: Int, advReqId: Int) {
    if (rowsDeleted != mustBe)
      throw new IllegalStateException(s"Adv request $advReqId not deleted. Rows deleted = $rowsDeleted, but 1 expected.")
  }


  /**
   * Посчитать цену размещения рекламной карточки согласно переданной спеке.
   * @param mad Размещаемая рекламная карточка.
   * @param adves2 Требования по размещению карточки на узлах.
   */
  def getAdvPrices(mad: MNode, adves2: List[AdvFormEntry]): Future[MAdvPricing] = {
    // 2015.nov.11 Метод стал очень асинхронным, хотя до этого был вообще синхронным.
    val someTrue = Some(true)
    // Параллельно собираем данные о ценниках для всех контрактов узла.
    val pricesFut = Future.traverse(adves2) { adve =>
      val contractsFut = Future {
        db.withConnection { implicit c =>
          MContract1.findForAdn(adve.adnId, isActive = someTrue)
        }
      }(AsyncUtil.jdbcExecutionContext)

      val rcvrContractFut = for (contracts <- contractsFut) yield {
        contracts
          .sortBy(_.id.get)
          .head
      }

      for {
        mbc <- rcvrContractFut
        mmp  <- {
          val contractId = mbc.id.get
          Future {
            db.withConnection { implicit c =>
              MTariffDaily.getLatestForContractId(contractId).get
            }
          }(AsyncUtil.jdbcExecutionContext)
        }
        mcalCtx <- {
          for {
            weekend <- getUrlCalAsync(mmp.weekendCalId)
            prime   <- getUrlCalAsync(mmp.primeCalId)
          } yield {
            MCals1Ctx(weekend = weekend, prime = prime)
          }
        }
      } yield {
        val bmc = getAdModulesCount(mad)
        calculateAdvPrice(bmc, mmp, adve, mcalCtx)
      }
    }

    val prodIdOpt = n2NodesUtil.madProducerId(mad)

    // Узнаём финансовое состояние кошелька узла-продьюсера карточки.
    val mbbFut = FutureUtil.optFut2futOpt(prodIdOpt) { prodId =>
      Future {
        db.withConnection { implicit c =>
          MBalance.getByAdnId(prodId)
        }
      }(AsyncUtil.jdbcExecutionContext)
    } map {
      _.get
    }

    // Строим список цен с валютами. По идее тут должна быть ровно одна цена.
    val prices2Fut = for (prices <- pricesFut) yield {
      prices
        .groupBy {
          _.currency.getCurrencyCode
        }
        .valuesIterator
        .map { p =>
          MPrice(p.map(_.price).sum, p.head.currency)
        }
        .toSeq
    }

    // Когда всё готово, определить достаточность бабла и вернуть ответ.
    for {
      mbb       <- mbbFut
      prices2   <- prices2Fut
    } yield {
      // Если есть разные валюты, то операция уже невозможна.
      val hasEnoughtMoney = prices2.size <= 1 && {
        prices2.headOption.exists { price =>
          price.currency.getCurrencyCode == mbb.currencyCode && price.amount <= mbb.amount.toDouble
        }
      }
      MAdvPricing(prices2, hasEnoughtMoney)
    }
  }


  /**
   * Сохранить в БД реквесты размещения рекламных карточек.
   * @param mad рекламная карточка.
   * @param advs Список запросов на размещение.
   */
  def mkAdvReqs(mad: MNode, advs: List[AdvFormEntry]): Future[_] = {
    val producerId = n2NodesUtil.madProducerId(mad).get

    val node2mbcMapFut = Future {
      db.withConnection { implicit c =>
        advs.iterator
          .map { adv =>
            val adnId = adv.adnId
            val mbc = getOrCreateContract(adnId)
            adnId -> mbc
          }
          .toMap
      }
    }(AsyncUtil.jdbcExecutionContext)

    val contractId2mmpMapFut = node2mbcMapFut.map { node2mbcMap =>
      db.withConnection { implicit c =>
        val iter = for {
          mbc         <- node2mbcMap.valuesIterator
          contractId  <- mbc.id
          mmp         <- MTariffDaily.getLatestForContractId(contractId)
        } yield {
          contractId -> mmp
        }
        iter.toMap
      }
    }(AsyncUtil.jdbcExecutionContext)

    val calsMapFut = contractId2mmpMapFut.flatMap { mmpMap =>
      val calIds = mmpMap.valuesIterator
        .flatMap { mmp =>
          Seq(mmp.primeCalId, mmp.weekendCalId)
        }
        .toSet
      Future.traverse(calIds) { calId =>
        for (calMan <- getUrlCalAsync(calId)) yield {
          calId -> calMan
        }
      } map {
        _.toMap
      }
    }

    for {
      node2mbcMap       <- node2mbcMapFut
      contractId2mmpMap <- contractId2mmpMapFut
      calsMap           <- calsMapFut
      res               <- {
        Future {
          db.withTransaction { implicit c =>
            // Вешаем update lock на баланс чтобы избежать блокирования суммы, списанной в параллельном треде, и дальнейшего ухода в минус.
            val mbb0 = MBalance.getByAdnId(producerId, SelectPolicies.UPDATE).get
            val someTrue = Some(true)
            val mbc = MContract.findForAdn(producerId, isActive = someTrue).head
            val prodCurrencyCode = mbb0.currencyCode
            advs.foreach { advEntry =>
              val rcvrContract = node2mbcMap(advEntry.adnId)
              val contractId = rcvrContract.id.get
              val rcvrMmp = contractId2mmpMap(contractId)
              val bmc = getAdModulesCount(mad)
              // Готовим календари
              val mcalCtx = MCals1Ctx(
                weekend = calsMap(rcvrMmp.weekendCalId),
                prime   = calsMap(rcvrMmp.primeCalId)
              )
              // Фильтруем уровни отображения в рамках sink'а.
              val advPrice = calculateAdvPrice(bmc, rcvrMmp, advEntry, mcalCtx)
              val rcvrCurrencyCode = advPrice.currency.getCurrencyCode
              assert(
                rcvrCurrencyCode == prodCurrencyCode,
                s"Rcvr node ${advEntry.adnId} currency ($rcvrCurrencyCode) does not match to producer node $producerId currency ($prodCurrencyCode)"
              )
              MAdvReq(
                adId        = mad.id.get,
                amount      = advPrice.price,
                prodContractId = mbc.id.get,
                prodAdnId   = producerId,
                rcvrAdnId   = advEntry.adnId,
                dateStart   = advEntry.dateStart.toDateTimeAtStartOfDay,
                dateEnd     = date2DtAtEndOfDay(advEntry.dateEnd),
                showLevels  = advEntry.showLevels
              ).save
              // Нужно заблокировать на счете узла необходимую сумму денег.
              mbb0.updateBlocked(advPrice.price)
            }
          }
        }(AsyncUtil.jdbcExecutionContext)
      }

    } yield {
      res
    }


  }

  def date2DtAtEndOfDay(ld: LocalDate) = ld.toDateTimeAtStartOfDay.plusDays(1).minusSeconds(1)


  /**
   * Стрёмная функция для получения активного контракта. Создаёт такой контракт, если его нет.
   * @param adnId id узла, с которым нужен контракт.
   * @return Экземпляр MContract.
   */
  private def getOrCreateContract(adnId: String)(implicit c: Connection): MContract1 = {
    MContract1.findForAdn(adnId, isActive = Some(true))
      .sortBy(_.id.get)
      .headOption
      .getOrElse {
        MContract1(adnId = adnId, contractDate = DateTime.now).save
      }
  }


  /**
   * Провести по БД прямую инжекцию рекламной карточки в выдачи.
   * А потом проверялка оффлайновых карточек отправит их в выдачу.
   * @param mad Рекламные карточки.
   * @param advs Список размещений.
   */
  def mkAdvsOk(mad: MNode, advs: List[AdvFormEntry]): Future[List[MAdvOk]] = {
    val producerId = n2NodesUtil.madProducerId(mad).get
    Future {
      db.withTransaction { implicit c =>
        advs map { advEntry =>
          MAdvOk(
            adId        = mad.id.get,
            amount      = 0F,
            prodAdnId   = producerId,
            rcvrAdnId   = advEntry.adnId,
            dateStart   = advEntry.dateStart.toDateTimeAtStartOfDay,
            dateEnd     = date2DtAtEndOfDay(advEntry.dateEnd),
            showLevels  = advEntry.showLevels,
            dateStatus  = DateTime.now(),
            prodTxnId   = None,
            rcvrTxnIds  = Nil,
            isOnline    = false,
            isPartner   = true,
            isAuto      = false
          ).save
        }
      }
    }(AsyncUtil.jdbcExecutionContext)
  }


  /**
   * Провести MAdvReq в MAdvOk, списав и зачислив все необходимые деньги.
   * @param advReq Одобряемый реквест размещения.
   * @param isAuto Пользователь одобряет или система?
   * @return Сохранённый экземпляр MAdvOk.
   */
  def acceptAdvReq(advReq: MAdvReq, isAuto: Boolean): MAdvOk = {
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
      assert(prodContractOpt.exists(_.id.get == advReq.prodContractId), "Producer contract not found or changed since request creation.")
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
   * @param advReq Запрос размещения.
   * @return Экземпляр сохранённого MAdvRefuse.
   */
  def refuseAdvReq(advReq: MAdvReq, advRefuse: MAdvRefuse): Future[_] = {
    Future {
      db.withTransaction { implicit c =>
        refuseAdvReqTxn(advReq, advRefuse)
      }
    }(AsyncUtil.jdbcExecutionContext)
  }
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
