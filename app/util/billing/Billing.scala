package util.billing

import controllers.SysMarketBilling
import models._
import play.api.Play.{current, configuration}
import play.api.db.DB
import org.joda.time.{Period, DateTime}
import util.anorm.AnormPgInterval
import scala.concurrent.duration._
import util.{CronTasksProvider, PlayMacroLogsImpl}
import AnormPgInterval._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 18.04.14 12:09
 * Description: Утиль для работы с биллингом, с финансовыми транзакциями.
 */
object Billing extends PlayMacroLogsImpl with CronTasksProvider {

  import LOGGER._

  /** Включено ли периодическое списание денег по крону? */
  def CRON_FEE_CHECK_ENABLED: Boolean = configuration.getBoolean("tariff.apply.every.enabled") getOrElse true

  /** Запускать тарификацию каждые n времени. */
  def TARIFFICATION_EVERY_MINUTES: Int = configuration.getInt("tariff.apply.every.minutes").getOrElse(20)
  val TARIFFICATION_PERIOD = new Period(0, TARIFFICATION_EVERY_MINUTES, 0, 0)
  def SCHED_TARIFFICATION_DURATION = TARIFFICATION_EVERY_MINUTES minutes


  /** Набор периодических задач для крона. */
  override def cronTasks = {
    if (CRON_FEE_CHECK_ENABLED) {
      val task = CronTask(startDelay = 10 seconds, every = SCHED_TARIFFICATION_DURATION, displayName = "processFeeTarificationAll()") {
        processFeeTarificationAll()
      }
      Seq(task)
    } else {
      Nil
    }
  }


  /** Инициализировать биллинг на узле. */
  def maybeInitializeNodeBilling(adnId: String) {
    lazy val logPrefix = s"maybeInitializeNodeBilling($adnId): "
    val newMbcOpt: Option[MBillContract] = DB.withTransaction { implicit c =>
      MBillContract.lockTableWrite
      val currentContracts = MBillContract.findForAdn(adnId)
      if (currentContracts.isEmpty) {
        val mbc = MBillContract(adnId = adnId).save
        Some(mbc)
      } else {
        None
      }
    }
    if (newMbcOpt.isDefined) {
      info(logPrefix + "Created new contract for node: " + newMbcOpt.get)
    } else {
      trace(logPrefix + "Node already have at least one contract.")
    }
    val newMbbOpt: Option[MBillBalance] = DB.withTransaction { implicit c =>
      MBillBalance.lockTableWrite
      if (MBillBalance.getByAdnId(adnId).isEmpty) {
        val mbb = MBillBalance(adnId, amount = 0F).save
        Some(mbb)
      } else {
        None
      }
    }
    if (newMbbOpt.isDefined) {
      info(logPrefix + "Created zero balance for node: " + newMbbOpt.get)
    } else {
      trace(logPrefix + "Node already have initialized balance.")
    }
  }

  /**
   * Обработать платеж внутри sql-транзакции.
   * @param txn Экземпляр ещё не сохранённой платежной транзакции [[models.MBillTxn]].
   * @return Обновлённые и сохранённые данные по финансовой операции.
   */
  def addPayment(txn: MBillTxn): AddPaymentResult = {
    DB.withTransaction { implicit c =>
      val contract = MBillContract.getById(txn.contractId).get
      // SelectPolicies.UPDATE не требуется, т.к. фактический инкремент идёт на стороне базы, а не тут.
      val balance0 = MBillBalance.getByAdnId(contract.adnId) getOrElse {
        MBillBalance(contract.adnId, 0F).save
      }
      if (txn.currencyCode != balance0.currencyCode)
        throw new UnsupportedOperationException(s"Currency convertion (${txn.currencyCode} -> ${balance0.currencyCode}) not yet implemented.")
      val txnSaved = txn.save
      val newBalance = balance0.updateAmount(txn.amount)
      AddPaymentResult(txnSaved, newBalance, contract)
    }
  }

  case class AddPaymentResult(txnSaved: MBillTxn, newBalance: MBillBalance, contract: MBillContract)


  /**
   * Пробежаться по активным тарифам активных договоров, проверив даты последнего списания.
   * Если период dateLast - now() превышает тарифицируемый период, то надо попытаться списать деньги.
   * Если пробит overdraft, то попробовать в другой раз.
   */
  def processFeeTarificationAll() {
    val feeTariffs = DB.withConnection { implicit c =>
      MBillTariffFee.findAllNonDebitedContractActive
    }
    // Есть на руках раскладка по тарифам. Пора пройтись по активным тарифам и попытаться выполнить списания.
    if (feeTariffs.nonEmpty) {
      debug(s"processFeeTarificationAll(): There are ${feeTariffs.size} fee-tariffs to process...")
      val contracts = DB.withConnection { implicit c =>
        MBillContract.findAllActive
      }
      val contractsMap = contracts
        .map { mbc => mbc.id.get -> mbc }
        .toMap
      feeTariffs.foreach { tariff =>
        try {
          val tariffContract = contractsMap(tariff.contractId)
          processFeeTarification(tariff, tariffContract)
        } catch {
          case ex: Exception => error("processFeeTarificationAll(): Failed to process tariff " + tariff, ex)
        }
      }
    }
  }

  /** Произвести тарификацию для одного тарифа в рамках одного договора. */
  def processFeeTarification(tariff: MBillTariffFee, contract: MBillContract) {
    lazy val logPrefix = s"processFeeTarification(${tariff.id.get}, ${tariff.contractId}): "
    val result: Either[String, FeeTarificationSuccess] = DB.withTransaction { implicit c =>
      val balanceOpt = MBillBalance.getByAdnId(contract.adnId, SelectPolicies.UPDATE)
        // Нельзя списывать деньги, если на счету нет денег.
        .filter { mbb =>
        // TODO Надо ругаться как-то на валюту
        val currencyMatches = mbb.currencyCode == tariff.feeCC
        if (!currencyMatches)
          warn(s"${logPrefix}Cannot debit $mbb because tariff has different currency: $tariff")
        val hasMoney = mbb.amount - tariff.fee > mbb.overdraft
        currencyMatches && hasMoney
      }
      balanceOpt match {
        // Пора оформить списание бабла по тарифу.
        case Some(balance) =>
          // Нужно определить дату списания.
          val now = DateTime.now()
          val datePaid = detectNewDatePaid(tariff, now)
          val txn0 = MBillTxn(
            contractId = tariff.contractId,
            amount     = -tariff.fee,
            currencyCodeOpt = Some(tariff.feeCC),
            datePaid   = datePaid,
            txnUid     = genTariffTxnUid(tariff, now),
            paymentComment = s"Payment under the contract ${contract.legalContractId}."
          )
          val txn = txn0.save
          // Транзакция есть. Надо обновить баланс.
          val newBalance = balance.updateAmount(-tariff.fee)
          val tariffsUpdated = MBillTariffFee.updateDebit(tariff.id.get, datePaid)
          if (tariffsUpdated != 1)
            throw new IllegalStateException(s"Unexpected invalid tariff state update result: $tariffsUpdated rows updated, but 1 expected. Rollback.")
          // По-скорее завершаем транзакцию, возвращая из этой функции положительный результат:
          Right(FeeTarificationSuccess(txn, newBalance))

        // Нет денег на балансе или какая-то другая проблема с балансом.
        case None =>
          Left(s"processFeeTarification(): tariff=${tariff.id.get} Cannot debit funds because no money.")
          // TODO Нет денег - надо блокировать рекламный узел (если ещё не заблокирован),
          //      слать письма и принимать разные прочие меры (по возможности - вне sql-транзакции).
      }
    }
    result match {
      case Right(success) =>
        info(s"${logPrefix}Successfully debited contract ${contract.legalContractId} by ${tariff.fee} ${tariff.feeCC}. :: $success")

      case Left(reason) =>
        warn(reason)
    }
  }
  case class FeeTarificationSuccess(txn: MBillTxn, newBalance: MBillBalance)


  /** Определение даты-времени списания. Если была задержка, то now. Иначе считаем относительно dateLast. */
  def detectNewDatePaid(tariff: MBillTariffFee, now: DateTime = DateTime.now): DateTime = {
    tariff.dateLast
      .filter { dateLast =>
        // Если была просрочка, то выкидываем эту дату
        val dateLastNext = dateLast.plus(tariff.tinterval)
        val dateLastCheck = now.minus(TARIFFICATION_PERIOD)
        val isExpired = dateLastNext.isAfter(dateLastCheck)
        !isExpired
      }
      .map { dateLast =>
        dateLast.plus(TARIFFICATION_PERIOD)
      }
      .getOrElse(now)
  }

  /** Генератор uid платежной транзакции. Она должна быть уникальной в рамках таблицы, но при этом не рандомной.
    * @param tariff Тариф.
    * @param now Текущее время.
    * @return
    */
  def genTariffTxnUid(tariff: MBillTariff, now: DateTime): String = {
    val sb = new StringBuilder
    val delimiter = '-'
    sb.append(tariff.contractId).append(delimiter)
      .append(tariff.id.get).append(delimiter)
      .append(now.getYear).append(delimiter)
      // Поколение записывается с целью отрабатывать возможные изменения tinterval без ломания уникальности.
      .append(tariff.generation).append(delimiter)
      .append(tariff.debitCount)
      .toString()
  }

}


