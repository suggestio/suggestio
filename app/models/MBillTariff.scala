package models

import anorm._
import org.joda.time.DateTime
import util.AnormJodaTime._
import util.AnormPgInterval._
import util.{FormUtil, SqlModelSave}
import java.sql.Connection
import java.util.Currency
import org.postgresql.util.PGInterval

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 21.04.14 16:19
 * Description: Модель тарифов при биллинге в рамках postgresql.
 */
object MBillTariff {
  import SqlParser._

  val ttypeP = get[String]("ttype") map { BTariffTypes.withName }

  /** Базовый парсер ряда без унаследованных полей. */
  val BASE_ROW_PARSER = {
    get[Pk[Int]]("id") ~ get[Int]("contract_id") ~ get[String]("name") ~ ttypeP ~
      get[Boolean]("is_enabled") ~ get[DateTime]("date_first") ~ get[DateTime]("date_created") ~
      get[Option[DateTime]]("date_modified") ~ get[Option[DateTime]]("date_last") ~ get[PGInterval]("tinterval") ~
      get[DateTime]("date_status") ~ get[Int]("generation") ~ get[Int]("debit_count")
  }

}


/** Допустимые тарифы. */
object BTariffTypes extends Enumeration {
  type BTariffType = Value
  val Fee = Value("f")

  def maybeWithName(n: String): Option[BTariffType] = {
    try {
      Some(withName(n))
    } catch {
      case ex: NoSuchElementException => None
    }
  }
}


trait TariffsFindByContract[T] extends SiowebSqlModelStatic[T] {
  /**
   * Найти все тарифы для указанного номера договора.
   * @param contractId id договора.
   * @return Список тарифов в неопределённом порядке.
   */
  def findByContractId(contractId: Int)(implicit c: Connection): List[T] = {
    SQL("SELECT * FROM " + TABLE_NAME + " WHERE contract_id = {contractId}")
     .on('contractId -> contractId)
     .as(rowParser *)
  }
}

/** Добавить функции нахождения всех активных тарифов. */
trait TariffsAllEnabled[T] extends SiowebSqlModelStatic[T] {
  def findAllEnabled(implicit c: Connection): List[T] = {
    SQL("SELECT * FROM " + TABLE_NAME + " WHERE is_enabled")
      .as(rowParser *)
  }

  /** Расширенная версия findAllEnabled: залезает в contracts-таблицу и проверяет там isEnabled. */
  def findAllContractEnabled(implicit c: Connection): List[T] = {
    SQL(s"SELECT t.* FROM $TABLE_NAME t, ${MBillContract.TABLE_NAME} c WHERE t.is_enabled AND c.is_active AND t.contract_id = c.id")
      .as(rowParser *)
  }

  /** Найти тарифы, которые нуждаются в скорейшем списании. Это enabled-тарифы, которые имеют период меньше,
    * чем now - последнее списание. */
  def findAllNonDebited(implicit c: Connection): List[T] = {
    SQL("SELECT t.* FROM " + TABLE_NAME + " t WHERE is_enabled AND date_last + tinterval < now()")
      .as(rowParser *)
  }

  /**
   * Найти все включенные тарифы, у который активные контракты и по которым пора бы списывать деньги.
   * Гибрид [[findAllContractEnabled]] и [[findAllNonDebited]].
   * @return Список тарифов в неопределённом порядке.
   */
  def findAllNonDebitedContractActive(implicit c: Connection): List[T] = {
    SQL(s"SELECT t.* FROM $TABLE_NAME t, ${MBillContract.TABLE_NAME} c WHERE t.is_enabled AND c.is_active AND t.contract_id = c.id AND (date_last IS NULL OR date_last + tinterval < now())")
      .as(rowParser *)
  }
}


trait UpdateDebitCount {
  def TABLE_NAME: String

  /**
   * Обновить счетчик debit_count и date_last с помощью инкремента до указанного значения.
   * Происходит во время дебета баланса (списания) по тарифу.
   * @param tariffId id тарифа.
   * @param incr Опциональный инкремент счетчика.
   * @return Кол-во обновлённых рядов, т.е. 0 или 1.
   */
  def updateDebit(tariffId: Int, dateLast: DateTime, incr: Int = 1)(implicit c: Connection): Int = {
    SQL("UPDATE " + TABLE_NAME + " SET debit_count = debit_count + {incr}, date_last = {dateLast} WHERE id = {id}")
      .on('id -> tariffId, 'incr -> incr, 'dateLast -> dateLast)
      .executeUpdate()
  }
}


object MBillTariffFee extends TariffsFindByContract[MBillTariffFee] with TariffsAllEnabled[MBillTariffFee] with UpdateDebitCount {
  import SqlParser._

  override val TABLE_NAME: String = "bill_tariff_fee"

  override val rowParser = {
    MBillTariff.BASE_ROW_PARSER ~ get[Float]("fee") ~ get[String]("fee_cc") map {
      case id ~ contractId ~ name ~ ttype ~ isEnabled ~ dateFirst ~ dateCreated ~ dateModified ~ dateLast ~ tinterval ~
           dateStatus ~ generation ~ debitCount ~ fee ~ feeCC =>
        // TODO Надо как-то использовать MBillTariffFee.apply без перечисления аргументов, порядок которых точно совпадает.
        MBillTariffFee(
          id          = id,
          contractId  = contractId,
          name        = name,
          ttype       = ttype,
          isEnabled   = isEnabled,
          dateFirst   = dateFirst,
          dateCreated = dateCreated,
          dateModified = dateModified,
          dateLast    = dateLast,
          tinterval   = tinterval,
          dateStatus  = dateStatus,
          generation  = generation,
          debitCount  = debitCount,
          fee         = fee,
          feeCC       = feeCC
        )
    }
  }

}

/** Интерфейс экземпляров тарифных моделей. */
trait MBillTariff {
  def contractId  : Int
  def name        : String
  def ttype       : BTariffType
  def isEnabled   : Boolean
  def dateFirst   : DateTime
  def tinterval   : PGInterval
  def dateCreated : DateTime
  def dateStatus  : DateTime
  def dateModified: Option[DateTime]
  def dateLast    : Option[DateTime]
  def id          : Pk[Int]
  def generation  : Int
  def debitCount  : Int
}


case class MBillTariffFee(
  id              : Pk[Int] = NotAssigned,
  var contractId  : Int,
  var name        : String,
  ttype           : BTariffType = BTariffTypes.Fee,
  var isEnabled   : Boolean,
  var dateFirst   : DateTime,
  dateCreated     : DateTime = DateTime.now,
  var dateModified: Option[DateTime] = None,
  dateLast        : Option[DateTime] = None,
  var tinterval   : PGInterval,
  var dateStatus  : DateTime = DateTime.now,
  generation      : Int = 0,
  debitCount      : Int = 0,
  var fee         : Float,
  var feeCC       : String = "RUB"
) extends SqlModelSave[MBillTariffFee] with MBillContractSel with SiowebSqlModel[MBillTariffFee] with MBillTariff {
  import MBillTariffFee._

  override def companion = MBillTariffFee

  def tintervalPretty: String = FormUtil.pgIntervalPretty(tinterval)

  /** Доступен ли ключ ряда в текущем инстансе? */
  override def hasId: Boolean = id.isDefined

  override def saveInsert(implicit c: Connection): MBillTariffFee = {
    SQL("INSERT INTO " + TABLE_NAME + "(contract_id, name, ttype, is_enabled, date_first, tinterval, date_created, date_status, generation, debit_count, fee, fee_cc) " +
        "VALUES ({contractId}, {name}, {ttype}, {isEnabled}, {dateFirst}, {tinterval}, {dateCreated}, {dateStatus}, {generation}, {debitCount}, {fee}, {feeCC})")
      .on('contractId -> contractId, 'name -> name, 'ttype -> ttype.toString, 'isEnabled -> isEnabled,
          'dateFirst -> dateFirst, 'tinterval -> tinterval, 'dateCreated -> dateCreated, 'dateStatus -> dateStatus,
          'generation -> generation, 'debitCount -> debitCount, 'fee -> fee, 'feeCC -> feeCC)
      .executeInsert(rowParser single)
  }

  override def saveUpdate(implicit c: Connection): Int = {
    SQL("UPDATE " + TABLE_NAME + " SET name = {name}, is_enabled = {isEnabled}, date_first = {dateFirst}," +
        " tinterval = {tinterval}, date_status = {dateStatus}, fee = {fee}, fee_cc = {feeCC}, generation = generation + 1" +
        " WHERE id = {id}")
      .on('id -> id.get, 'name -> name, 'isEnabled -> isEnabled, 'dateFirst -> dateFirst, 'tinterval -> tinterval,
          'dateStatus -> dateStatus, 'fee -> fee, 'feeCC -> feeCC)
      .executeUpdate()
  }

  def feeCurrency = Currency.getInstance(feeCC)
}

