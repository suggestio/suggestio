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
 * Created: 14.05.14 18:59
 * Description: Модель тарифов, где списания происходят по просмотрам/кликам (статистика).
 * Сама статистика по факту в списаниях не участвует, но списания жестко кореллируют с ней.
 */
object MBillTariffStat extends TariffsFindByContract[MBillTariffStat] with TariffsAllEnabled[MBillTariffStat] {
  import SqlParser._

  override val TABLE_NAME = "bill_tariff_stat"

  val debitForParser = get[String]("debit_for") map {
    AdStatActions.withName
  }

  override val rowParser: RowParser[MBillTariffStat] = {
    MBillTariff.BASE_ROW_PARSER ~ get[Float]("debited_total") ~ debitForParser ~ get[Float]("debit_amount") ~ get[String]("currency_code") map {
      case id ~ contractId ~ name ~ ttype ~ isEnabled ~ dateFirst ~ dateCreated ~ dateModified ~ dateLast ~
           dateStatus ~ generation ~ debitCount ~ debitedTotal ~ debitFor ~ debitAmount ~ currencyCode =>
        MBillTariffStat(
          id          = id,
          contractId  = contractId,
          name        = name,
          ttype       = ttype,
          isEnabled   = isEnabled,
          dateFirst   = dateFirst,
          dateCreated = dateCreated,
          dateModified = dateModified,
          dateLast    = dateLast,
          dateStatus  = dateStatus,
          generation  = generation,
          debitCount  = debitCount,
          debitedFor  = debitFor,
          debitAmount = debitAmount,
          debitedTotal = debitedTotal,
          currencyCode = currencyCode
        )
    }
  }
}



case class MBillTariffStat(
  id              : Pk[Int] = NotAssigned,
  var contractId  : Int,
  var name        : String,
  debitedFor      : AdStatAction,
  debitAmount     : Float,
  ttype           : BTariffType = BTariffTypes.Fee,
  var isEnabled   : Boolean,
  var dateFirst   : DateTime,
  dateCreated     : DateTime = DateTime.now,
  var dateModified: Option[DateTime] = None,
  dateLast        : Option[DateTime] = None,
  var dateStatus  : DateTime = DateTime.now,
  generation      : Int = 0,
  debitCount      : Int = 0,
  debitedTotal    : Float = 0F,
  var currencyCode: String = "RUB"
) extends SqlModelSave[MBillTariffStat] with MBillContractSel with SiowebSqlModel[MBillTariffStat] with MBillTariff {
  import MBillTariffStat._

  override def companion = MBillTariffStat

  /** Доступен ли ключ ряда в текущем инстансе? */
  override def hasId: Boolean = id.isDefined

  override def saveInsert(implicit c: Connection): MBillTariffStat = {
    SQL("INSERT INTO " + TABLE_NAME + "(contract_id, name, ttype, is_enabled, date_first, date_created, date_status, generation, debit_count, debited_total, debit_for, debit_amount, currency_code) " +
        "VALUES ({contractId}, {name}, {ttype}, {isEnabled}, {dateFirst}, {dateCreated}, {dateStatus}, {generation}, {debitCount}, {debitedTotal}, {debitFor}, {debitAmount}, {currencyCode})")
      .on('contractId -> contractId, 'name -> name, 'ttype -> ttype.toString, 'isEnabled -> isEnabled,
          'dateFirst -> dateFirst, 'dateCreated -> dateCreated, 'dateStatus -> dateStatus,
          'generation -> generation, 'debitCount -> debitCount, 'debitFor -> debitedFor.toString,
          'debitAmount -> debitAmount, 'currencyCode -> currencyCode)
      .executeInsert(rowParser single)
  }

  override def saveUpdate(implicit c: Connection): Int = {
    SQL("UPDATE " + TABLE_NAME + " SET name = {name}, is_enabled = {isEnabled}, date_first = {dateFirst}," +
        " date_status = {dateStatus}, debit_amount = {debitAmount}, currency_code = {currencyCode}," +
        " generation = generation + 1 WHERE id = {id}")
      .on('id -> id.get, 'name -> name, 'isEnabled -> isEnabled, 'dateFirst -> dateFirst,
          'dateStatus -> dateStatus, 'debitAmount -> debitAmount, 'currencyCode -> currencyCode)
      .executeUpdate()
  }

}
