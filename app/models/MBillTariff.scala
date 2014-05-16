package models

import anorm._
import org.joda.time.DateTime
import util.AnormJodaTime._
import java.sql.Connection
import util.AnormPgArray._

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
      get[Option[DateTime]]("date_modified") ~ get[Option[DateTime]]("date_last") ~
      get[DateTime]("date_status") ~ get[Int]("generation") ~ get[Int]("debit_count")
  }

}


/** Допустимые тарифы. */
object BTariffTypes extends Enumeration {
  type BTariffType = Value
  val Fee = Value("f")
  val Stat = Value("s")

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
  def findByContractId(contractId: Int, policy: SelectPolicy = SelectPolicies.NONE)(implicit c: Connection): List[T] = {
    val sb = new StringBuilder("SELECT * FROM ").append(TABLE_NAME).append(" WHERE contract_id = {contractId}")
    policy.append2sb(sb)
    SQL(sb.toString())
     .on('contractId -> contractId)
     .as(rowParser *)
  }

  def findByContractIds(contractIds: Traversable[Int], policy: SelectPolicy = SelectPolicies.NONE)(implicit c: Connection): List[T] = {
    val sb = new StringBuilder("SELECT * FROM ").append(TABLE_NAME).append(" WHERE contract_id = ANY({contractIds})")
    policy.append2sb(sb)
    SQL(sb.toString())
      .on('contractIds -> seqInt2pgArray(contractIds))
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


/** Интерфейс экземпляров тарифных моделей. */
trait MBillTariff {
  def contractId  : Int
  def name        : String
  def ttype       : BTariffType
  def isEnabled   : Boolean
  def dateFirst   : DateTime
  def dateCreated : DateTime
  def dateStatus  : DateTime
  def dateModified: Option[DateTime]
  def dateLast    : Option[DateTime]
  def id          : Pk[Int]
  def generation  : Int
  def debitCount  : Int
}

