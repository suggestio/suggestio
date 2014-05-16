package util.billing

import akka.actor._
import util.{SiowebSup, PlayMacroLogsImpl}
import models._
import play.api.Play.{current, configuration}
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import scala.concurrent.duration._
import scala.concurrent.future
import play.api.db.DB
import scala.util.{Failure, Success}
import play.libs.Akka
import org.joda.time.DateTime

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 15.05.14 14:47
 * Description: Для серьезного снижения нагрузки на БД и (в будущем) для отправки изменений на akka-узел с pg-мастером,
 * используется этот актор. От накапливает некоторое кол-во просмотров/переходов/др, и затем пакетно накатывает
 * их на тарификацию.
 */

object StatBillingQueueActor extends PlayMacroLogsImpl {
  import LOGGER._

  /** Интервал сброса аккамулятора данных. */
  private val FLUSH_INTERVAL = (configuration.getInt("billing.stat.queue.flush.interval.seconds") getOrElse 60).seconds

  val ACTOR_NAME = classOf[StatBillingQueueActor].getSimpleName
  
  def startLink(arf: ActorRefFactory): ActorRef = {
    arf.actorOf(Props[StatBillingQueueActor], name=ACTOR_NAME)
  }
 
  val ACTOR_PATH = SiowebSup.actorPath / ACTOR_NAME

  def actorSelection = Akka.system.actorSelection(ACTOR_PATH)
 
  /** Асинхронно отправить новую статистику актору. */
  def sendNewStats(rcvrId: String, mad: MAdT, action: AdStatAction) {
    actorSelection ! NewStats(rcvrId, mad, action)
  }
  
  /**
   * Проведение всех накопленных платежей разом.
   * @param acc Аккамулятор просмотров/переходов.
   * @return Сколько денег было списано в сумме.
   */
  private def flushAcc(acc: List[NewStats]): Map[String, Float] = {
    val producerIds = acc.map(_.mad.producerId).distinct
    val accMap = acc.map { ns => (ns.mad.producerId -> ns.action) -> ns }.groupBy(_._1)
    // Синхронное накатывание всех списаний в рамках одной транзакции.
    // TODO Следует разбить транзакцию на последовательность транзакций в рамках каждого adnId?
    DB.withTransaction { implicit c =>
      // В голове собираем всю информацию по тарификации. Тарификаторы блокируем для апдейта.
      val balances = MBillBalance.getByAdnIds(producerIds)
        .map { mbb => mbb.adnId -> mbb }
        .toMap
      val contracts = MBillContract.findForAdns(producerIds)
        .map { mbc => mbc.id.get -> mbc }
        .toMap
      val contractIds = contracts.keys
      val statTariffs = MBillTariffStat.findByContractIds(contractIds, SelectPolicies.UPDATE)
      // Собираем карту для быстрого поиска тарификации на основе полей NewStats.
      val statTariffsMap = statTariffs
        .flatMap { st =>
          contracts.get(st.contractId)
            .map { mbc =>
              val k = mbc.adnId -> st.debitFor
              k -> st
            }
        }
        .toMap
      val now = DateTime.now()
      // Сохраняем точку в транзакции, на которую можно откатится при ошибке в первой итерации. Изменений по факту ещё никаких не произошло.
      val savepointCounter0 = 0
      PgTransaction.savepoint(savepointName(savepointCounter0))
      // Акк содержит счетчик чекпоинтов транзакции (формировать имя savepoint'а) и список проведённых списаний для построения отчета.
      accMap.foldLeft [(Int, List[(String, Float)])] ((savepointCounter0 + 1) -> Nil) {
        case (fullAcc @ (i, debitAcc), (k, nss)) =>
          // try изолирует ошибки в одном шаге транзакции от остальных шагов по списанию с других счетов. По-хорошему это стоит сделать это через набор транзакций, но это будет значительно более ресурсоемко.
          try {
            statTariffsMap
              .get(k)
              .orElse {
                warn("statTariffs does not countain tariff for (adnId, action) = " + k)
                None
              }
              .filter { mbts =>
                val result = mbts.dateFirst.isAfter(now)
                if (!result)
                  info(s"Skipping stat tariff ${mbts.id.get}, because dateFirst in future (${mbts.dateFirst})")
                result
              }
              // Сначала узнаём, сколько вообще денег можно списать, нельзя юзера загонять в овердрафт. Для этого нужен баланс
              .flatMap { mbts =>
                balances.get(k._1)
                  .map { mbts -> _ }
              }
              // Если валюта не совпадает, то операция невозможна.
              .filter { case (mbts, mbb) =>
                val result = mbb.currencyCode == mbts.currencyCode
                if (!result)
                  warn(s"flushAcc1(): Unable to withdraw balance because currency convertion not supported. balance=$mbb tariff=$mbts")
                result
              }
              // Провести списание
              .map { case (mbts, mbb) =>
                // Тарификатор и баланс найдены, всё готово. Проводим все оплаты разом. Нужно обновить ballance и сам TariffStat
                val canDebitMax = mbb.amount - mbb.overdraft
                val nssSz = nss.size
                val needDebit = nssSz * mbts.debitAmount
                val willDebit: Float = Math.min(needDebit, canDebitMax)
                // обновляем балансы
                mbts.updateDebited(nssSz, willDebit)
                mbb.updateAmount(-willDebit)
                PgTransaction.savepoint(savepointName(i))
                // Формируем результат для аккамулятора
                mbts.currencyCode -> willDebit
              }
              // Закинуть значение опционального результата в аккамулятор, инкрементить счетчик.
              .fold(fullAcc) { e => (i + 1, e :: debitAcc)}
          } catch {
            case ex: Exception =>
              val prevTxnPoint = savepointName(i - 1)
              error(s"[$i]Failed to debit node account, (adnId, action) = $k. Rollbacking to previous txn savepoint '$prevTxnPoint' and continue.", ex)
              PgTransaction.rollbackTo(prevTxnPoint)
              fullAcc
          }
      }
    }
      // Для формирования отчета, результат надо сгруппировать по валюте, и проссуммировать списания в рамках каждой валюты.
      ._2
      .groupBy(_._1)
      .mapValues { _.iterator.map(_._2).reduce(_ + _) }
  }

  private def savepointName(i: Int) = s"t$i"
}

import StatBillingQueueActor._


/** Актор для аккумуляции и периодического сброса данных. */
class StatBillingQueueActor extends Actor {
  import LOGGER._

  protected var acc: List[NewStats] = Nil

  protected var flushTimer: Option[Cancellable] = None


  override def receive: Receive = {
    // Поступает новая статистика.
    case ns: NewStats =>
      acc ::= ns
      ensureFlushTimer()

    // Сработал таймер сборса аккамулятора.
    case FlushTimer =>
      trace(s"FlushTimer occured. There are ${acc.size} items to tarifficate.")
      // Крайне невероятно, что в этой проверке вообще есть какой-то смысл, т.к. flush-таймер запускается только когда появляются элементы.
      if (!acc.isEmpty) {
        val readyAcc = acc
        future {
          flushAcc(readyAcc)
        } onComplete {
          case Success(totalDebited) => trace("Successfully flushed accumulator. Debited: " + totalDebited.mkString(", "))
          case Failure(ex)           => error(s"Failed to debit ${acc.size} items: $readyAcc", ex)
        }
        acc = Nil
      }
  }


  // Внутренние функции.

  /** Убедится, что таймер сброса данных запущен. */
  protected def ensureFlushTimer() {
    if (flushTimer.isEmpty) {
      trace("ensureFlushTimer(): Starting new flush timer: flush_interval = " + FLUSH_INTERVAL)
      val timer = context.system.scheduler.scheduleOnce(FLUSH_INTERVAL) {
        self ! FlushTimer
      }
      flushTimer = Some(timer)
    }
  }

  protected case object FlushTimer
}


sealed case class NewStats(rcvrId: String, mad: MAdT, action: AdStatAction)
