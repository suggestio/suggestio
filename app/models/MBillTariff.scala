package models

import anorm._
import org.joda.time.DateTime
import util.AnormJodaTime._
import util.AnormPgInterval._
import util.SqlModelSave
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
      get[DateTime]("date_status")
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


object MBillTariffFee extends TariffsFindByContract[MBillTariffFee] {
  import SqlParser._

  override val TABLE_NAME: String = "bill_tariff_fee"

  override val rowParser = {
    MBillTariff.BASE_ROW_PARSER ~ get[Float]("fee") ~ get[String]("fee_cc") map {
      case id ~ contractId ~ name ~ ttype ~ isEnabled ~ dateFirst ~ dateCreated ~ dateModified ~ dateLast ~ tinterval ~ dateStatus ~ fee ~ feeCC =>
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
          fee         = fee,
          feeCC       = feeCC
        )
    }
  }

}


case class MBillTariffFee(
  var contractId  : Int,
  var name        : String,
  ttype           : BTariffType = BTariffTypes.Fee,
  var isEnabled   : Boolean,
  var dateFirst   : DateTime,
  var tinterval   : PGInterval,
  var fee         : Float,
  var feeCC       : String = "RUB",
  dateCreated     : DateTime = DateTime.now,
  var dateStatus  : DateTime = DateTime.now,
  var dateModified: Option[DateTime] = None,
  dateLast        : Option[DateTime] = None,
  id              : Pk[Int] = NotAssigned
) extends SqlModelSave[MBillTariffFee] with MBillContractSel with SiowebSqlModel[MBillTariffFee] {
  import MBillTariffFee._

  override def companion = MBillTariffFee

  /** Доступен ли ключ ряда в текущем инстансе? */
  override def hasId: Boolean = id.isDefined

  override def saveInsert(implicit c: Connection): MBillTariffFee = {
    SQL("INSERT INTO " + TABLE_NAME + "(contract_id, name, ttype, is_enabled, date_first, tinterval, date_created, date_status, fee, fee_cc) " +
        "VALUES ({contractId}, {name}, {ttype}, {isEnabled}, {dateFirst}, {tinterval}, {dateCreated}, {dateStatus}, {fee}, {feeCC})")
      .on('contractId -> contractId, 'name -> name, 'ttype -> ttype.toString, 'isEnabled -> isEnabled,
          'dateFirst -> dateFirst, 'tinterval -> tinterval, 'dateCreated -> dateCreated, 'dateStatus -> dateStatus,
          'fee -> fee, 'feeCC -> feeCC)
      .executeInsert(rowParser single)
  }

  override def saveUpdate(implicit c: Connection): Int = {
    SQL("UPDATE " + TABLE_NAME + " SET name = {name}, is_enabled = {isEnabled}, date_first = {dateFirst}, " +
        "tinterval = {tinterval}, date_status = {dateStatus}, fee = {fee}, fee_cc = {feeCC} WHERE id = {id}")
      .on('id -> id.get, 'name -> name, 'isEnabled -> isEnabled, 'dateFirst -> dateFirst, 'tinterval -> tinterval,
          'date_status -> dateStatus, 'fee -> fee, 'feeCC -> feeCC)
      .executeUpdate()
  }

  def feeCurrency = Currency.getInstance(feeCC)
}

