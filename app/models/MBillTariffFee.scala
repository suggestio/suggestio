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
 * Created: 14.05.14 18:58
 * Description: Тарифы для повременной оплаты.
 */

object MBillTariffFee extends TariffsFindByContract[MBillTariffFee] with TariffsAllEnabled[MBillTariffFee] with UpdateDebitCount {
  import SqlParser._

  override val TABLE_NAME: String = "bill_tariff_fee"

  override val rowParser = {
    MBillTariff.BASE_ROW_PARSER ~ get[Float]("fee") ~ get[String]("fee_cc") ~ get[PGInterval]("tinterval") map {
      case id ~ contractId ~ name ~ ttype ~ isEnabled ~ dateFirst ~ dateCreated ~ dateModified ~ dateLast ~
           dateStatus ~ generation ~ debitCount ~ fee ~ feeCC ~ tinterval =>
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

  def nextPayDt = dateLast.map(_ plus tinterval)
}
