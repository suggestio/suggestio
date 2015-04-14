package util.billing

import de.jollyday.parameter.UrlManagerParameter
import io.suggest.ym.model.common.EMBlockMetaI
import models._
import models.blk.{BlockWidths, BlockHeights}
import org.joda.time.{Period, DateTime, LocalDate}
import org.joda.time.DateTimeConstants._
import util.adv.AdvUtil
import scala.annotation.tailrec
import util.blocks.BlocksConf
import util.{CronTasksProvider, PlayMacroLogsImpl}
import play.api.db.DB
import play.api.Play.{current, configuration}
import io.suggest.ym.parsers.Price
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import scala.concurrent.Future
import util.SiowebEsUtil.client
import scala.util.{Success, Failure}
import util.event.SiowebNotifier.Implicts.sn
import java.sql.Connection
import io.suggest.ym.model.common.EMReceivers.Receivers_t
import scala.concurrent.duration._
import de.jollyday.HolidayManager
import java.net.URL
import controllers.{AdvFormEntry, routes}
import scala.collection.JavaConversions._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 28.05.14 19:04
 * Description: Утиль для работы с биллингом, где имеют вес площади и расценки получателя рекламы.
 */
object MmpDailyBilling extends PlayMacroLogsImpl with CronTasksProvider {

  import LOGGER._

  /** Включен ли биллинг по крону? Будет выполнятся публикация карточек, их сокрытие и т.д. */
  def CRON_BILLING_CHECK_ENABLED: Boolean = configuration.getBoolean("mmp.daily.check.enabled") getOrElse true

  /** Сколько раз пытаться повторять сохранение обновлённого списка ресиверов. */
  val UPDATE_RCVRS_VSN_CONFLICT_TRY_MAX = configuration.getInt("mmp.daily.save.update.rcvrs.onConflict.try.max") getOrElse 5

  /** Не раньше какого времени можно запускать auto-accept. */
  val AUTO_ACCEPT_REQS_AFTER_HOURS = configuration.getInt("mmp.daily.accept.auto.after.hours") getOrElse 16

  /** Как часто надо проверять таблицу advsOK на предмет необходимости изменений в выдаче. */
  def CHECK_ADVS_OK_DURATION: FiniteDuration = configuration.getInt("mmp.daily.check.advs.ok.every.seconds")
    .getOrElse(120)
    .seconds

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


  override def cronTasks = {
    if (CRON_BILLING_CHECK_ENABLED) {
      val every = CHECK_ADVS_OK_DURATION
      val applyOldReqs = CronTask(
        startDelay = 3.seconds,
        every = every,
        displayName = "autoApplyOldAdvReqs()"
      ) {
        autoApplyOldAdvReqs()
      }
      val depubExpired = CronTask(
        startDelay = 10.seconds,
        every = every,
        displayName = "depublishExpiredAdvs()"
      ) {
        depublishExpiredAdvs()
      }
      val advOfflineAdvs = CronTask(
        startDelay = 30.seconds,
        every = every,
        displayName = "advertiseOfflineAds()"
      ) {
        advertiseOfflineAds()
      }
      List(applyOldReqs, depubExpired, advOfflineAdvs)

    } else {
      Nil
    }
  }

  /**
   * Рассчитать ценник размещения рекламной карточки.
   * Цена блока рассчитывается по площади, тарифам размещения узла-получателя и исходя из будней-праздников.
   * @return
   */
  def calculateAdvPrice(blockModulesCount: Int, rcvrPricing: MBillMmpDaily, advTerms: AdvTerms): Price = {
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
        case LVL_CATS         => rcvrPricing.onRcvrCat * amountN
        case LVL_START_PAGE   => rcvrPricing.onStartPage * amountN
        case LVL_PRODUCER     => amountN
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
  def getAdModulesCount(mad: EMBlockMetaI): Int = {
    lazy val logPrefix = s"getAdModulesCount(${mad.id.getOrElse("?")}): "
    val block: BlockConf = BlocksConf applyOrDefault mad.blockMeta.blockId
    // Мультипликатор по ширине
    val wmul = BlockWidths(mad.blockMeta.width).relSz
    // Мультипликатор по высоте
    val hmul = BlockHeights(mad.blockMeta.height).relSz
    val blockModulesCount: Int = wmul * hmul
    trace(s"${logPrefix}blockModulesCount = $wmul * $hmul = $blockModulesCount ;; blockId = ${mad.blockMeta.blockId}")
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
  def getAdvPrices(mad: MAd, adves2: List[AdvFormEntry]): MAdvPricing = {
    DB.withConnection { implicit c =>
      val someTrue = Some(true)
      val prices = adves2.foldLeft[List[Price]] (Nil) { (acc, adve) =>
        val rcvrContract = MBillContract.findForAdn(adve.adnId, isActive = someTrue)
          .sortBy(_.id.get)
          .head
        val contractId = rcvrContract.id.get
        val rcvrPricing = MBillMmpDaily.getLatestForContractId(contractId).get
        val bmc = getAdModulesCount(mad)
        val advPrice = MmpDailyBilling.calculateAdvPrice(bmc, rcvrPricing, adve)
        advPrice :: acc
      }
      val prices2 = prices
        .groupBy { _.currency.getCurrencyCode }
        .mapValues { p => p.head.currency -> p.map(_.price).sum }
        .values
      val mbb = MBillBalance.getByAdnId(mad.producerId).get
      // Если есть разные валюты, то операция уже невозможна.
      val hasEnoughtMoney = prices2.size <= 1 && {
        prices2.headOption.exists { price =>
          price._1.getCurrencyCode == mbb.currencyCode  &&  price._2 <= mbb.amount
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
  def mkAdvReqs(mad: MAd, advs: List[AdvFormEntry]) {
    import mad.producerId
    DB.withTransaction { implicit c =>
      // Вешаем update lock на баланс чтобы избежать блокирования суммы, списанной в параллельном треде, и дальнейшего ухода в минус.
      val mbb0 = MBillBalance.getByAdnId(producerId, SelectPolicies.UPDATE).get
      val someTrue = Some(true)
      val mbc = MBillContract.findForAdn(producerId, isActive = someTrue).head
      val prodCurrencyCode = mbb0.currencyCode
      advs.foreach { advEntry =>
        val rcvrContract = getOrCreateContract(advEntry.adnId)
        val contractId = rcvrContract.id.get
        val rcvrMmp = MBillMmpDaily.getLatestForContractId(contractId).get
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
  }

  def date2DtAtEndOfDay(ld: LocalDate) = ld.toDateTimeAtStartOfDay.plusDays(1).minusSeconds(1)


  /**
   * Стрёмная функция для получения активного контракта. Создаёт такой контракт, если его нет.
   * @param adnId id узла, с которым нужен контракт.
   * @return Экземпляр [[models.MBillContract]].
   */
  private def getOrCreateContract(adnId: String)(implicit c: Connection): MBillContract = {
    MBillContract.findForAdn(adnId, isActive = Some(true))
      .sortBy(_.id.get)
      .headOption
      .getOrElse {
        MBillContract(adnId = adnId, contractDate = DateTime.now).save
      }
  }


  /**
   * Провести по БД прямую инжекцию рекламной карточки в выдачи.
   * А потом проверялка оффлайновых карточек отправит их в выдачу.
   * @param mad Рекламные карточки.
   * @param advs Список размещений.
   */
  def mkAdvsOk(mad: MAd, advs: List[AdvFormEntry]): List[MAdvOk] = {
    import mad.producerId
    DB.withTransaction { implicit c =>
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
    DB.withTransaction { implicit c =>
      // Удалить исходный реквест размещения
      val reqsDeleted = advReq.delete
      assertAdvsReqRowsDeleted(reqsDeleted, 1, advReqId)
      // Провести все денежные операции.
      val prodAdnId = advReq.prodAdnId
      val prodContractOpt = MBillContract.findForAdn(prodAdnId, isActive = Some(true)).headOption
      assert(prodContractOpt.exists(_.id.get == advReq.prodContractId), "Producer contract not found or changed since request creation.")
      val prodContract = prodContractOpt.get
      val amount0 = advReq.amount

      // Списать заблокированную сумму:
      val oldProdMbbOpt = MBillBalance.getByAdnId(prodAdnId)
      assert(oldProdMbbOpt.exists(_.currencyCode == advReq.currencyCode), "producer balance currency does not match to adv request")
      val prodMbbUpdated = MBillBalance.updateBlocked(prodAdnId, -amount0)
      assert(prodMbbUpdated == 1, "Failed to debit blocked amount for producer " + prodAdnId)

      val now = DateTime.now
      // Запилить единственную транзакцию списания для продьюсера
      val prodTxn = MBillTxn(
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
      val rcvrContract = MBillContract.findForAdn(rcvrAdnId, isActive = Some(true))
        .headOption
        .getOrElse {
          warn(s"advReqAcceptSubmit($advReqId): Creating new contract for adv. award receiver...")
          MBillContract(
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
        .foldLeft( List.empty[MBillTxn] ) { case (acc, (sink, sinkShowLevels)) =>
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
            val rcvrMbb = MBillBalance.getByAdnId(rcvrAdnId) getOrElse MBillBalance(rcvrAdnId, 0F, Some(advReq.currencyCode))
            assert(rcvrMbb.currencyCode == advReq.currencyCode, "Rcvr balance currency does not match to adv request")
            // Зачислить деньги на счет получателя. Для списаний с разной комиссией нужны разные транзакции.
            val rcvrMbb2 = rcvrMbb.updateAmount(amount1)
            val rcvrTxn = MBillTxn(
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
   * @param reason Причина отказа.
   * @return Экземпляр сохранённого MAdvRefuse.
   */
  def refuseAdvReq(advReq: MAdvReq, reason: String): MAdvRefuse = {
    val advRefused = MAdvRefuse(advReq, reason, DateTime.now)
    DB.withTransaction { implicit c =>
      val rowsDeleted = advReq.delete
      assertAdvsReqRowsDeleted(rowsDeleted, 1, advReq.id.get)
      val advr = advRefused.save
      // Разблокировать средства на счёте.
      MBillBalance.blockAmount(advr.prodAdnId, -advr.amount)  // TODO Нужно ли округлять, чтобы blocked в минус не уходило из-за неточностей float/double?
      // Вернуть сохранённый refused
      advr
    }
  }


  /** Цикл автоматического накатывания MAdvReq в MAdvOk. Нужно найти висячие MAdvReq и заапрувить их. */
  def autoApplyOldAdvReqs() {
    val period = new Period(AUTO_ACCEPT_REQS_AFTER_HOURS, 0, 0, 0)
    val advsReq = DB.withConnection { implicit c =>
      MAdvReq.findCreatedLast(period)
    }
    //val logPrefix = "autoApplyOldAdvReqs(): "
    if (advsReq.nonEmpty) {
      // TODO Нужно оттягивать накатывание карточки до ближайшего обеда. Для этого нужно знать тайм-зону для рекламных узлов.
      advsReq.foreach(acceptAdvReq(_, isAuto = true))
    }
  }


  /** Выполнить поиск и размещение в выдачах рекламных карточек, время размещения которых уже пришло. */
  def advertiseOfflineAds() {
    AdvertiseOfflineAdvs.run()
  }

  /** Выполнить поиск и сокрытие в выдачах рекламных карточках, время размещения которых истекло. */
  def depublishExpiredAdvs() {
    DepublishExpiredAdvs.run()
  }


  /**
   * Пересчёт текущих размещений указанной рекламной карточки на основе данных MAdvOk online.
   * @param adId id рекламной карточки.
   * @return Карта ресиверов.
   */
  def calcualteReceiversMapForAd(adId: String): Receivers_t = {
    val advsOk = DB.withConnection { implicit c =>
      MAdvOk.findOnlineFor(adId, isOnline = true)
    }
    advsOk
      .map { advOk =>
        AdReceiverInfo(advOk.rcvrAdnId, advOk.showLevels)
      }
      // Размещение одной карточки на одном узле на разных уровнях может быть проведено через разные размещения.
      .groupBy(_.receiverId)
      .mapValues { _.reduceLeft[AdReceiverInfo] {
        (prev, next) => next.copy(sls = next.sls ++ prev.sls) }
      }
  }

}


/** Код вывода в выдачу и последующего сокрытия рекламных карточек крайне похож, поэтому он вынесен в трейт. */
sealed trait AdvSlsUpdater extends PlayMacroLogsImpl {

  import LOGGER._

  lazy val logPrefix = ""

  def findAdvsOk(implicit c: Connection): List[MAdvOk]

  def nothingToDo() {}

  def updateAdvOk(advOk: MAdvOk, now: DateTime): MAdvOk

  def run() {
    val advs = DB.withConnection { implicit c =>
      findAdvsOk
    }
    if (advs.nonEmpty) {
      trace(s"${logPrefix}Where are ${advs.size} items. ids = ${advs.map(_.id.get).mkString(", ")}")
      val advsMap = advs.groupBy(_.adId)
      val now = DateTime.now
      advsMap foreach { case (adId, advsOk) =>
        val advsOk1 = advsOk
          .map { updateAdvOk(_, now) }
        DB.withTransaction { implicit c =>
          advsOk1.foreach(_.save)
        }
        // Запустить пересчёт уровней отображения для затронутой рекламной карточки.
        val madUpdFut = MAd.getById(adId) flatMap {
          case Some(mad0) =>
            // Запускаем полный пересчет карты ресиверов.
            AdvUtil.calculateReceiversFor(mad0) flatMap { rcvrs1 =>
              // Новая карта ресиверов готова. Заливаем её в карточку и сохраняем последнюю.
              MAd.tryUpdate(mad0) { mad1 =>
                mad1.copy(
                  receivers = rcvrs1
                )
              }
            }

          // Карточка внезапно не найдена. Наверное она была удалена, а инфа в базе почему-то осталась. В любом случае, нужно удалить все adv.
          case None =>
            val totalDeleted = DB.withConnection { implicit c =>
              MAdv.deleteByAdId(adId)
            }
            warn(s"${logPrefix}Ad[$adId] not exists, but at least ${advsOk.size} related advs in processing. I've removed $totalDeleted orphan advs from all adv models!")
            Future successful adId
        }
        madUpdFut onFailure { case ex =>
          error(s"${logPrefix}Failed to update ad[$adId] rcvrs. Rollbacking advOks update txn...", ex)
          try {
            DB.withConnection { implicit c =>
              advs.foreach(_.save)
            }
            debug(s"${logPrefix}Successfully recovered adv state back for ad[$adId]. Will retry update on next time.")
          } catch {
            case ex: Throwable => error(s"${logPrefix}Failed to rollback advOks update txn! Adv state inconsistent. Advs:\n  $advs", ex)
          }
        }
      }
    } else {
      nothingToDo()
    }
  }
}


/** Обновлялка adv sls, добавляющая уровни отображаения к существующей рекламе, которая должна бы выйти в свет. */
object AdvertiseOfflineAdvs extends AdvSlsUpdater {

  override def findAdvsOk(implicit c: Connection): List[MAdvOk] = {
    MAdvOk.findAllOfflineOnTime
  }

  override def updateAdvOk(advOk: MAdvOk, now: DateTime): MAdvOk = {
    val dateDiff = new Period(advOk.dateStart, now)
    advOk.copy(
      dateStart = now,
      dateEnd = advOk.dateEnd plus dateDiff,
      isOnline = true
    )
  }
}


/** Обновлялка adv sls, которая снимает уровни отображения с имеющейся рекламы, которая должна уйти из выдачи
  * по истечению срока размещения. */
object DepublishExpiredAdvs extends AdvSlsUpdater {

  override def findAdvsOk(implicit c: Connection): List[MAdvOk] = {
    MAdvOk.findDateEndExpired()
  }

  override def updateAdvOk(advOk: MAdvOk, now: DateTime): MAdvOk = {
    advOk.copy(
      isOnline = false,
      dateEnd = now
    )
  }
}

