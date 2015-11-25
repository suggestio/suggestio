package models.mbill

import java.sql.Connection
import java.util.Currency

import anorm._
import models._
import models.stat.{ScStatAction, ScStatActions}
import org.joda.time.DateTime
import util.anorm.AnormJodaTime._
import util.sqlm.SqlModelSave

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 14.05.14 18:59
 * Description: Модель тарифов, где списания происходят по просмотрам/кликам (статистика).
 * Сама статистика по факту в списаниях не участвует, но списания жестко кореллируют с ней.
 */
object MTariffStat extends FindByContract with TariffsAllEnabled {
  import SqlParser._

  override type T = MTariffStat
  override val TABLE_NAME = "bill_tariff_stat"

  val debitForParser = get[String]("debit_for") map {
    ScStatActions.withName
  }

  override val rowParser: RowParser[MTariffStat] = {
    MTariff.BASE_ROW_PARSER ~ get[Float]("debited_total") ~ debitForParser ~ get[Float]("debit_amount") ~ get[String]("currency_code") map {
      case id ~ contractId ~ name ~ ttype ~ isEnabled ~ dateFirst ~ dateCreated ~ dateModified ~ dateLast ~
           dateStatus ~ generation ~ debitCount ~ debitedTotal ~ debitFor ~ debitAmount ~ currencyCode =>
        MTariffStat(
          id           = id,
          contractId   = contractId,
          name         = name,
          ttype        = ttype,
          isEnabled    = isEnabled,
          dateFirst    = dateFirst,
          dateCreated  = dateCreated,
          dateModified = dateModified,
          dateLast     = dateLast,
          dateStatus   = dateStatus,
          generation   = generation,
          debitCount   = debitCount,
          debitFor     = debitFor,
          debitAmount  = debitAmount,
          debitedTotal = debitedTotal,
          currencyCode = currencyCode
        )
    }
  }


  def updateDebited(id: Int, debitedCount: Int, willDebit: Float)(implicit c: Connection): Int = {
    SQL("UPDATE " + TABLE_NAME + " SET debit_count = debit_count + {debitedCount}, debited_total = debited_total + {willDebit} WHERE id = {id}")
      .on('id -> id, 'debitedCount -> debitedCount, 'willDebit -> willDebit)
      .executeUpdate()
  }

}



final case class MTariffStat(
  id              : Option[Int] = None,
  contractId      : Int,
  name            : String,
  debitFor        : ScStatAction,
  debitAmount     : Float,
  ttype           : BTariffType = BTariffTypes.Stat,
  isEnabled       : Boolean,
  dateFirst       : DateTime,
  dateCreated     : DateTime = DateTime.now,
  dateModified    : Option[DateTime] = None,
  dateLast        : Option[DateTime] = None,
  dateStatus      : DateTime = DateTime.now,
  generation      : Int = 0,
  debitCount      : Int = 0,
  debitedTotal    : Float = 0F,
  currencyCode    : String = "RUB"
) extends SqlModelSave with MContractSel with SqlModelDelete with MTariff {
  import MTariffStat._

  override type T = MTariffStat

  override def companion = MTariffStat

  /** Доступен ли ключ ряда в текущем инстансе? */
  override def hasId: Boolean = id.isDefined

  override def saveInsert(implicit c: Connection): T = {
    SQL("INSERT INTO " + TABLE_NAME + "(contract_id, name, ttype, is_enabled, date_first, date_created, date_status, generation, debit_count, debited_total, debit_for, debit_amount, currency_code) " +
        "VALUES ({contractId}, {name}, {ttype}, {isEnabled}, {dateFirst}, {dateCreated}, {dateStatus}, {generation}, {debitCount}, {debitedTotal}, {debitFor}, {debitAmount}, {currencyCode})")
      .on('contractId -> contractId, 'name -> name, 'ttype -> ttype.toString, 'isEnabled -> isEnabled,
          'dateFirst -> dateFirst, 'dateCreated -> dateCreated, 'dateStatus -> dateStatus,
          'generation -> generation, 'debitCount -> debitCount, 'debitedTotal -> debitedTotal, 'debitFor -> debitFor.toString,
          'debitAmount -> debitAmount, 'currencyCode -> currencyCode)
      .executeInsert(rowParser single)
  }

  override def saveUpdate(implicit c: Connection): Int = {
    SQL("UPDATE " + TABLE_NAME + " SET name = {name}, is_enabled = {isEnabled}, date_first = {dateFirst}," +
        " date_status = {dateStatus}, debit_for = {debitFor}, debit_amount = {debitAmount}, currency_code = {currencyCode}," +
        " generation = generation + 1 WHERE id = {id}")
      .on('id -> id.get, 'name -> name, 'isEnabled -> isEnabled, 'dateFirst -> dateFirst,
          'dateStatus -> dateStatus, 'debitFor -> debitFor.toString, 'debitAmount -> debitAmount, 'currencyCode -> currencyCode)
      .executeUpdate()
  }

  def updateDebited(debitedCount: Int, willDebit: Float)(implicit c: Connection) = {
    MTariffStat.updateDebited(
      id = id.get,
      debitedCount = debitedCount,
      willDebit = willDebit
    )
  }

  def currency = Currency.getInstance(currencyCode)
}
