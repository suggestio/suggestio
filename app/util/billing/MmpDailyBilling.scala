package util.billing

import io.suggest.ym.model.common.EMBlockMetaI
import models._
import org.joda.time.{Period, DateTime, LocalDate}
import org.joda.time.DateTimeConstants._
import scala.annotation.tailrec
import util.blocks.{BfHeight, BlocksUtil, BlocksConf}
import util.PlayMacroLogsImpl
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
object MmpDailyBilling extends PlayMacroLogsImpl {

  import LOGGER._

  /** Сколько раз пытаться повторять сохранение обновлённого списка ресиверов. */
  val UPDATE_RCVRS_VSN_CONFLICT_TRY_MAX = configuration.getInt("mmp.daily.save.update.rcvrs.onConflict.try.max") getOrElse 5

  /** Не раньше какого времени можно запускать auto-accept. */
  val AUTO_ACCEPT_REQS_AFTER_HOURS = configuration.getInt("mmp.daily.accept.auto.after.hours") getOrElse 16

  /** Как часто надо проверять таблицу advsOK на предмет необходимости изменений в выдаче. */
  val CHECK_ADVS_OK_DURATION: FiniteDuration = configuration.getInt("mmp.daily.check.advs.ok.every.seconds")
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
  private def getUrlCal(calId: String) = {
    HolidayManager.getInstance(
      new URL(MYSELF_URL_PREFIX + routes.SysCalendar.getCalendarXml(calId))
    )
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
    var amountTotal: Float = amountN
    if (advTerms hasOnAdSl AdShowLevels.LVL_CATS) {
      val incr = rcvrPricing.onRcvrCat * amountN
      amountTotal += incr
      trace(s"$logPrefix +rcvrCat: +x${rcvrPricing.onRcvrCat} == +$incr -> $amountTotal")
    }
    if (advTerms hasOnAdSl AdShowLevels.LVL_START_PAGE) {
      val incr = rcvrPricing.onStartPage * amountN
      amountTotal += incr
      trace(s"$logPrefix +onStartPage: +x${rcvrPricing.onStartPage} == +$incr -> $amountTotal")
    }
    trace(s"$logPrefix amount (1/N/Total) = $amount1 / $amountN / $amountTotal")
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
      case BfHeight.HEIGHT_140 => 1
      case BfHeight.HEIGHT_300 => 2
      case BfHeight.HEIGHT_460 => 3
      case BfHeight.HEIGHT_620 => 4
      case other =>
        warn(logPrefix + "Unexpected block height: " + other)
        1
    }
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
        findSinkComms(contractId, adve)
          .valuesIterator
          .foldLeft (acc) { (acc1, sinkComm) =>
            val adve1 = advTermSinkFiltered(sinkComm.sink, adve)
            val advPrice = MmpDailyBilling.calculateAdvPrice(bmc, rcvrPricing, adve1)
            advPrice :: acc1
          }
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
    * @param advTerms настройки размещения.
    * @return Карта тарифов комиссионных, подходящая под указанное добро.
    */
  private def findSinkComms(rcvrContractId: Int, advTerms: AdvTerms)(implicit c: Connection): Map[AdnSink, MSinkComission] = {
    MSinkComission.findByContractId(rcvrContractId)
      .groupBy(_.sink)
      .filterKeys { advTerms.hasOnSink }    // Выкинуть sink'и, которые рекламодателю не интересны.
      .mapValues(_.head)                    // Там всегда один элемент из-за особенностей модели.
  }

  private def advTermSinkFiltered(sink: AdnSink, advt: AdvTerms): AdvTerms = {
    new AdvTermsWrapper {
      override def underlying = advt
      override val showLevels = advt.showLevels.filter(_.adnSink == sink)
    }
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
        val sinkComms = findSinkComms(contractId, advEntry)
        sinkComms.valuesIterator.foreach { sinkComm =>
          // Фильтруем уровни отображения в рамках sink'а.
          val advTermsSink = advTermSinkFiltered(sinkComm.sink, advEntry)
          val advPrice = calculateAdvPrice(bmc, rcvrMmp, advTermsSink)
          val rcvrCurrencyCode = advPrice.currency.getCurrencyCode
          assert(
            rcvrCurrencyCode == prodCurrencyCode,
            s"Rcvr node ${advEntry.adnId} currency ($rcvrCurrencyCode) does not match to producer node $producerId currency ($prodCurrencyCode)"
          )
          MAdvReq(
            adId        = mad.id.get,
            amount      = advPrice.price,
            comission   = Some(sinkComm.sioComission),
            prodContractId = mbc.id.get,
            prodAdnId   = producerId,
            rcvrAdnId   = advEntry.adnId,
            dateStart   = advEntry.dateStart.toDateTimeAtStartOfDay,
            dateEnd     = date2DtAtEndOfDay(advEntry.dateEnd),
            showLevels  = advTermsSink.showLevels
          ).save
          // Нужно заблокировать на счете узла необходимую сумму денег.
          mbb0.updateBlocked(advPrice.price)
        }
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
          comission   = None,
          prodAdnId   = producerId,
          rcvrAdnId   = advEntry.adnId,
          dateStart   = advEntry.dateStart.toDateTimeAtStartOfDay,
          dateEnd     = date2DtAtEndOfDay(advEntry.dateEnd),
          showLevels  = advEntry.showLevels,
          dateStatus  = DateTime.now(),
          prodTxnId   = None,
          rcvrTxnId   = None,
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
      // Запилить транзакцию списания для продьюсера
      val prodTxn = MBillTxn(
        contractId      = prodContract.id.get,
        amount          = -amount0,
        datePaid        = advReq.dateCreated,
        txnUid          = s"${prodContract.id.get}-$advReqId",
        paymentComment  = "Debit for advertise",
        dateProcessed   = now
      ).save

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
      // Начислять получателю бабки с учётом комиссии sioM.
      val amount1 = advReq.comission.fold(amount0) { comission =>  (1.0F - comission) * amount0 }
      assert(amount1 <= amount0, "Comissioned amount must be less or equal than source amount.")
      val rcvrMbbOpt = MBillBalance.getByAdnId(rcvrAdnId)
      assert(rcvrMbbOpt.exists(_.currencyCode == advReq.currencyCode), "Rcvr balance currency does not match to adv request")
      val rcvrMbb = rcvrMbbOpt getOrElse MBillBalance(rcvrAdnId, 0F, Some(advReq.currencyCode))

      // Зачислить деньги на счет получателя
      rcvrMbb.updateAmount(amount1)
      val rcvrTxn = MBillTxn(
        contractId      = rcvrContract.id.get,
        amount          = amount1,
        comissionPc     = advReq.comission,
        datePaid        = advReq.dateCreated,
        txnUid          = s"${rcvrContract.id.get}-$advReqId",
        paymentComment  = "Credit for adverise",
        dateProcessed   = now
      ).save

      // Сохранить подтверждённое размещение с инфой о платежах.
      MAdvOk(
        advReq,
        comission1  = advReq.comission,
        dateStatus1 = now,
        prodTxnId   = prodTxn.id,
        rcvrTxnId   = rcvrTxn.id,
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


  /** Логика добавления уровеня отображения */
  private def ensureProducerLevel(sls: Set[SinkShowLevel], sl: SinkShowLevel): Set[SinkShowLevel] = {
    if (sls.exists(_.adnSink == sl.adnSink)  &&  !sls.contains(sl)) {
      sls + sl
    } else {
      sls
    }
  }

  /** Нужно убеждаться, что есть producer-уровень в наборе уровней. */
  def prepareShowLevels(sls: Set[SinkShowLevel]): Set[SinkShowLevel] = {
    // Добавить wifi-producer sl, если есть любой другой уровень wifi-уровень.
    val sls1 = ensureProducerLevel(sls, SinkShowLevels.WIFI_PRODUCER_SL)
    // Добавить geo-producer sl, если есть любой другой geo-уровень отображения.
    ensureProducerLevel(sls1, SinkShowLevels.GEO_PRODUCER_SL)
  }

}

import MmpDailyBilling.prepareShowLevels


/** Код вывода в выдачу и последующего сокрытия рекламных карточек крайне похож, поэтому он вынесен в трейт. */
sealed trait AdvSlsUpdater extends PlayMacroLogsImpl {

  import LOGGER._

  lazy val logPrefix = ""

  def findAdvsOk(implicit c: Connection): List[MAdvOk]

  def updateReceivers(rcvrs0: Receivers_t, advsOk: List[MAdvOk]): Receivers_t

  def nothingToDo() {}

  def updateAdvOk(advOk: MAdvOk, now: DateTime): MAdvOk

  def run() {
    val advs = DB.withConnection { implicit c =>
      findAdvsOk
    }
    if (advs.nonEmpty) {
      trace(s"${logPrefix}Where are ${advs.size} items. ids = ${advs.map(_.id.get).mkString(", ")}")
      val advsMap = advs.groupBy(_.adId)
      advsMap foreach { case (adId, advsOk) =>
        // Определяем неблокирующую функцию получения и обновления рекламной карточки, которую можно многократно вызывать.
        // Повторная попытка получения и обновления карточки поможет разрулить конфликт версий.
        val madUpdFut = MAd.getById(adId) flatMap {
          case Some(mad0) =>
            MAd.tryUpdate(mad0) { mad1 =>
              mad1.copy(
                receivers = updateReceivers(mad0.receivers, advsOk)
              )
            }
          case None =>
            Future failed new RuntimeException(s"MAd not found: $adId, but it should. Cannot continue.")
        }
        madUpdFut onComplete {
          case Success(_) =>
            trace(s"${logPrefix}Updating isOnline state for ${advsOk.size} advsOk...")
            val now = DateTime.now
            val advsOk1 = advsOk
              .map { updateAdvOk(_, now) }
            DB.withTransaction { implicit c =>
              advsOk1.foreach(_.save)
            }
            trace(s"${logPrefix}Successfully updated state for ad.id=$adId for advs = ${advsOk.mkString(", ")}")

          case Failure(ex) =>
            error(s"${logPrefix}Failed to update ad $adId advs states", ex)
        }
      }
    } else {
      nothingToDo()
    }
  }
}


/** Обновлялка adv sls, добавляющая уровни отображаения к существующей рекламе, которая должна бы выйти в свет. */
object AdvertiseOfflineAdvs extends AdvSlsUpdater {
  import LOGGER._

  override def findAdvsOk(implicit c: Connection): List[MAdvOk] = {
    MAdvOk.findAllOfflineOnTime
  }

  /** Добавить новые ресиверы для рекламной карточки. */
  override def updateReceivers(rcvrs0: Receivers_t, advsOk: List[MAdvOk]): Receivers_t = {
    rcvrs0 ++ advsOk.foldLeft[List[(String, AdReceiverInfo)]](Nil) { (acc, advOk) =>
      trace(s"${logPrefix}Advertising ad ${advOk.adId} on rcvrNode ${advOk.rcvrAdnId}; advOk.id = ${advOk.id.get}")
      val sls2 = prepareShowLevels(advOk.showLevels)
      val rcvrInfo = rcvrs0.get(advOk.rcvrAdnId) match {
        case None =>
          AdReceiverInfo(advOk.rcvrAdnId, sls2)
        case Some(ri) =>
          // Всё уже готово вроде бы.
          ri.copy(sls = ri.sls ++ sls2)
      }
      advOk.rcvrAdnId -> rcvrInfo :: acc
    }
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
  import LOGGER._

  override def findAdvsOk(implicit c: Connection): List[MAdvOk] = {
    MAdvOk.findDateEndExpired()
  }

  /** Удалить ресиверы для рекламной карточки. */
  override def updateReceivers(rcvrs0: Receivers_t, advsOk: List[MAdvOk]): Receivers_t = {
    val adId = advsOk.headOption.fold("???")(_.adId)
    val rcvr2adv = advsOk
      .map { advOk => advOk.rcvrAdnId -> advOk }
      .toMap
    val rcvrs1 = rcvrs0.foldLeft[List[(String, AdReceiverInfo)]] (Nil) {
      case (acc, e @ (rcvrAdnId, rcvrInfo)) =>
        rcvr2adv.get(rcvrAdnId) match {
          // Работа не касается этого ресивера. Просто пропускаем его на выход.
          case None =>
            e :: acc

          // Есть ресивер. Надо подстричь его уровни отображения или всего ресивера целиком.
          case Some(advOk) =>
            trace(s"${logPrefix}Depublishing ad $adId on rcvrNode $rcvrAdnId ;; advOk.id = ${advOk.id}")
            // Восстановить уровни отображения, которые были задействованы при добавлении оных в ресиверы.
            val sls0 = prepareShowLevels(advOk.showLevels)
            // Нужно отфильтровать уровни отображения или целиком этот ресивер спилить.
            val sls2 = rcvrInfo.sls -- sls0
            if (sls2.isEmpty) {
              // Нет уровней отображения для этого ресивера.
              trace(s"updateReceivers(): Removing rcvr[$rcvrAdnId] from ad[$adId], because no more show levels available.")
              acc
            } else {
              // Ещё остались уровни для отображения. Запиливаем ресивер в кучу.
              trace(s"updateReceivers(): KEEPing rcvr[$rcvrAdnId] for ad[$adId], because there are some show levels: ${sls2.mkString(", ")}.")
              val e1 = rcvrInfo.copy(
                sls = sls2
              )
              rcvrAdnId -> e1  ::  acc
            }
        }
    }
    rcvrs1.toMap
  }

  override def updateAdvOk(advOk: MAdvOk, now: DateTime): MAdvOk = {
    advOk.copy(
      isOnline = false,
      dateEnd = now
    )
  }
}

