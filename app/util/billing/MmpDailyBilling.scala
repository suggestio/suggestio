package util.billing

import com.google.inject.{Inject, Singleton}
import de.jollyday.parameter.UrlManagerParameter
import io.suggest.common.fut.FutureUtil
import io.suggest.model.n2.edge.{MNodeEdges, MEdgeInfo}
import models._
import models.adv.direct.AdvFormEntry
import models.adv.{MAdvReq, MAdvRefuse, MAdvOk, IAdvTerms}
import models.adv.tpl.MAdvPricing
import models.blk.{BlockWidths, BlockHeights}
import models.mbill._
import org.joda.time.{DateTime, LocalDate}
import org.joda.time.DateTimeConstants._
import play.api.Configuration
import util.async.AsyncUtil
import util.n2u.N2NodesUtil
import scala.annotation.tailrec
import util.PlayMacroLogsImpl
import play.api.db.Database
import io.suggest.ym.parsers.Price
import java.sql.Connection
import de.jollyday.HolidayManager
import java.net.URL
import controllers.routes
import scala.collection.JavaConversions._
import scala.concurrent.{ExecutionContext, Future}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 28.05.14 19:04
 * Description: Утиль для работы с биллингом, где имеют вес площади и расценки получателя рекламы.
 */
@Singleton
class MmpDailyBilling @Inject() (
  n2NodesUtil             : N2NodesUtil,
  configuration           : Configuration,
  db                      : Database,
  implicit val ec         : ExecutionContext
)
  extends PlayMacroLogsImpl
{

  import LOGGER._

  val MYSELF_URL_PREFIX: String = configuration.getString("mmp.daily.localhost.url.prefix") getOrElse {
    val myPort = Option(System.getProperty("http.port")).fold(9000)(_.toInt)
    s"http://localhost:$myPort"
  }

  /** Если внезапно необходимо создать контракт, то сделать его с таким суффиксом. */
  lazy val CONTRACT_SUFFIX_DFLT = configuration.getString("mmp.daily.contract.suffix.dflt") getOrElse "СЕО"

  /** Дни недели, относящиеся к выходным. Задаются списком чисел от 1 (пн) до 7 (вс), согласно DateTimeConstants. */
  val WEEKEND_DAYS: Set[Int] = configuration.getIntList("mmp.daily.weekend.days").map(_.map(_.intValue).toSet) getOrElse Set(FRIDAY, SATURDAY, SUNDAY)


  /** Сгенерить localhost-ссылку на xml-текст календаря. */
  // TODO Надо бы асинхронно фетчить тело календаря и потом результат как-то заворачивать в URL, чтобы не было лишних блокировок.
  // Часть блокировок подавляет кеш на стороне jollyday.
  private def getUrlCal(calId: String): HolidayManager = {
    val calUrl = new URL(MYSELF_URL_PREFIX + routes.SysCalendar.getCalendarXml(calId))
    val args = new UrlManagerParameter(calUrl, null)
    HolidayManager.getInstance(args)
  }

  /**
   * Рассчитать ценник размещения рекламной карточки.
   * Цена блока рассчитывается по площади, тарифам размещения узла-получателя и исходя из будней-праздников.
   * @return
   */
  def calculateAdvPrice(blockModulesCount: Int, rcvrPricing: MTariffDaily, advTerms: IAdvTerms): Price = {
    lazy val logPrefix = s"calculateAdvPrice($blockModulesCount/${rcvrPricing.id.get}): "
    trace(s"${logPrefix}rcvr: tariffId=${rcvrPricing.id.get} mbcId=${rcvrPricing.contractId};; terms: from=${advTerms.dateStart} to=${advTerms.dateEnd} sls=${advTerms.showLevels}")
    // Во избежание бесконечного цикла, огораживаем dateStart <= dateEnd
    val dateStart = advTerms.dateStart
    val dateEnd = advTerms.dateEnd
    //assert(!(dateStart isAfter dateEnd), "dateStart must not be after dateEnd")
    // TODO Нужно использовать специфичные для узла календари.
    val primeHolidays = getUrlCal(rcvrPricing.weekendCalId)
    lazy val weekendCal = getUrlCal(rcvrPricing.primeCalId)
    def calculateDateAdvPrice(day: LocalDate): Float = {
      if (primeHolidays isHoliday day) {
        trace(s"$logPrefix $day -> primetime -> +${rcvrPricing.mmpPrimetime}")
        rcvrPricing.mmpPrimetime
      } else {
        val isWeekend = (WEEKEND_DAYS contains day.getDayOfWeek) || (weekendCal isHoliday day)
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
          MContract.findForAdn(adve.adnId, isActive = someTrue)
        }
      }(AsyncUtil.jdbcExecutionContext)

      val rcvrContractFut = for (contracts <- contractsFut) yield {
        contracts
          .sortBy(_.id.get)
          .head
      }

      for {
        rcvrContract <- rcvrContractFut
        rcvrPricing  <- {
          val contractId = rcvrContract.id.get
          Future {
            db.withConnection { implicit c =>
              MTariffDaily.getLatestForContractId(contractId).get
            }
          }(AsyncUtil.jdbcExecutionContext)
        }
      } yield {
        val bmc = getAdModulesCount(mad)
        calculateAdvPrice(bmc, rcvrPricing, adve)
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


  /** Найти список тарифов комиссионных для указанного размещения в рамках указанного контракта.
    * @param rcvrContractId id контракта.
    * @return Карта тарифов комиссионных, подходящая под указанное добро.
    */
  private def findSinkComms(rcvrContractId: Int)(implicit c: Connection): Map[AdnSink, MSinkComission] = {
    MSinkComission.findByContractId(rcvrContractId)
      .groupBy(_.sink)
      .mapValues(_.head)                    // Там всегда один элемент из-за особенностей модели.
  }

  /**
   * Сохранить в БД реквесты размещения рекламных карточек.
   * @param mad рекламная карточка.
   * @param advs Список запросов на размещение.
   */
  def mkAdvReqs(mad: MNode, advs: List[AdvFormEntry]): Future[_] = {
    val producerId = n2NodesUtil.madProducerId(mad).get
    Future {
      db.withTransaction { implicit c =>
        // Вешаем update lock на баланс чтобы избежать блокирования суммы, списанной в параллельном треде, и дальнейшего ухода в минус.
        val mbb0 = MBalance.getByAdnId(producerId, SelectPolicies.UPDATE).get
        val someTrue = Some(true)
        val mbc = MContract.findForAdn(producerId, isActive = someTrue).head
        val prodCurrencyCode = mbb0.currencyCode
        advs.foreach { advEntry =>
          val rcvrContract = getOrCreateContract(advEntry.adnId)
          val contractId = rcvrContract.id.get
          val rcvrMmp = MTariffDaily.getLatestForContractId(contractId).get
          val bmc = getAdModulesCount(mad)
          // Фильтруем уровни отображения в рамках sink'а.
          val advPrice = calculateAdvPrice(bmc, rcvrMmp, advEntry)
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

  def date2DtAtEndOfDay(ld: LocalDate) = ld.toDateTimeAtStartOfDay.plusDays(1).minusSeconds(1)


  /**
   * Стрёмная функция для получения активного контракта. Создаёт такой контракт, если его нет.
   * @param adnId id узла, с которым нужен контракт.
   * @return Экземпляр [[MContract]].
   */
  private def getOrCreateContract(adnId: String)(implicit c: Connection): MContract = {
    MContract.findForAdn(adnId, isActive = Some(true))
      .sortBy(_.id.get)
      .headOption
      .getOrElse {
        MContract(adnId = adnId, contractDate = DateTime.now).save
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
    val rcvrAdnId = advReq.rcvrAdnId
    val advReqId = advReq.id.get
    lazy val logPrefix = s"acceptAdvReq($advReqId): "
    trace(s"${logPrefix}Starting. isAuto=$isAuto advReq=$advReq")
    db.withTransaction { implicit c =>
      // Удалить исходный реквест размещения
      val reqsDeleted = advReq.delete
      assertAdvsReqRowsDeleted(reqsDeleted, 1, advReqId)
      // Провести все денежные операции.
      val prodAdnId = advReq.prodAdnId
      val prodContractOpt = MContract.findForAdn(prodAdnId, isActive = Some(true)).headOption
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

      // Разобраться с кошельком получателя
      val rcvrContract = MContract.findForAdn(rcvrAdnId, isActive = Some(true))
        .headOption
        .getOrElse {
          warn(s"advReqAcceptSubmit($advReqId): Creating new contract for adv. award receiver...")
          MContract(
            adnId         = rcvrAdnId,
            contractDate  = now,
            isActive      = true,
            suffix        = Some(CONTRACT_SUFFIX_DFLT),
            dateCreated   = now
          ).save
        }
      val rcvrContractId = rcvrContract.id.get
      // Начислять получателю бабки с учётом комиссии sioM. Нужно прочитать карту текущих комиссий.
      val mscsMap = findSinkComms(rcvrContractId)
      // Нужно провести транзакции, разделив уровни отображения по используемому sink'у.
      val advSsl = advReq.showLevels.groupBy(_.adnSink)
      // Все sink'и имеют одинаковый тариф. И в рамках одного реквеста одинаковый набор sl для каждого sink'а.
      // Чтобы не пересчитывать цену, можно просто поделить ей на кол-во используемых sink'ов
      assert(advSsl.valuesIterator.map(_.map(_.sl)).toSet.size == 1, "Different sls for different sinks not yet implemented.")
      val advSinkCount = advSsl.size
      val sinkAmount: Float = amount0 / advSinkCount
      val rcvrTxns = advSsl
        .foldLeft( List.empty[MTxn] ) { case (acc, (sink, sinkShowLevels)) =>
          // Считаем комиссированную цену в рамках sink'а. Если sink'а нет, то пусть будет экзепшен и всё.
          // 2015.feb.06: sink'и выдачи незаметно стали не нужны. Разрешаем дефолтовые значения, если тариф не задан.
          val msc: Float = mscsMap.get(sink) match {
            case Some(m) => m.sioComission
            case None    => sink.sioComissionDflt
          }
          if (msc > 0.998F) {
            // Если комиссия около 100%, то не проводить транзакцию для ресивера.
            trace(s"$logPrefix Comission is near 100%. Dropping transaction")
            acc
          } else {
            // Размер комиссии допускает отправку денег ресиверу. Считаем комиссию, обновляем кошелёк, проводим транзакцию для ресивера.
            // Сначала надо вычистить долю расхода в рамках текущего sink'а.
            val amount1 = (1.0F - msc) * sinkAmount
            assert(amount1 <= sinkAmount, "Comissioned amount must be less or equal than source amount.")
            val rcvrMbb = MBalance.getByAdnId(rcvrAdnId) getOrElse MBalance(rcvrAdnId, 0F, Some(advReq.currencyCode))
            assert(rcvrMbb.currencyCode == advReq.currencyCode, "Rcvr balance currency does not match to adv request")
            // Зачислить деньги на счет получателя. Для списаний с разной комиссией нужны разные транзакции.
            val rcvrMbb2 = rcvrMbb.updateAmount(amount1)
            val rcvrTxn = MTxn(
              contractId      = rcvrContractId,
              amount          = amount1,
              comissionPc     = Some(msc),
              datePaid        = advReq.dateCreated,
              txnUid          = s"$rcvrContractId-$advReqId-${sink.name}",
              paymentComment  = "Credit for adverise",
              dateProcessed   = now,
              currencyCodeOpt = Option(advReq.currencyCode)
            ).save
            trace(s"${logPrefix}Credited receiver[$rcvrAdnId] contract=$rcvrContractId for $amount1 ${advReq.currencyCode}. sink=$sink/$msc; Balance was ${rcvrMbb.amount} become ${rcvrMbb2.amount} ; tnxId=${rcvrTxn.id.get}")
            rcvrTxn :: acc
          }
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
