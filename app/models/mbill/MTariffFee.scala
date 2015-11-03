package models.mbill

import java.sql.Connection
import java.util.Currency

import anorm._
import models._
import org.joda.time.DateTime
import org.postgresql.util.PGInterval
import util.anorm.AnormJodaTime._
import util.anorm.AnormPgInterval._
import util.{FormUtil, SqlModelSave}
/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 14.05.14 18:58
 * Description: Тарифы для повременной оплаты.
 */

object MTariffFee extends FindByContract with TariffsAllEnabled with UpdateDebitCount {
  import SqlParser._

  override type T = MTariffFee

  override val TABLE_NAME: String = "bill_tariff_fee"

  override val rowParser = {
    MTariff.BASE_ROW_PARSER ~ get[Float]("fee") ~ get[String]("fee_cc") ~ get[PGInterval]("tinterval") map {
      case id ~ contractId ~ name ~ ttype ~ isEnabled ~ dateFirst ~ dateCreated ~ dateModified ~ dateLast ~
           dateStatus ~ generation ~ debitCount ~ fee ~ feeCC ~ tinterval =>
        // TODO Надо как-то использовать MBillTariffFee.apply без перечисления аргументов, порядок которых точно совпадает.
        MTariffFee(
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


  /** Найти тарифы, которые нуждаются в скорейшем списании. Это enabled-тарифы, которые имеют период меньше,
    * чем now - последнее списание. */
  def findAllNonDebited(implicit c: Connection): List[MTariffFee] = {
    SQL("SELECT t.* FROM " + TABLE_NAME + " t WHERE is_enabled AND date_last + tinterval < now()")
      .as(rowParser *)
  }

  /**
   * Найти все включенные тарифы, у который активные контракты и по которым пора бы списывать деньги.
   * Гибрид [[findAllContractEnabled]] и [[findAllNonDebited]].
   * @return Список тарифов в неопределённом порядке.
   */
  def findAllNonDebitedContractActive(implicit c: Connection): List[MTariffFee] = {
    SQL(s"SELECT t.* FROM $TABLE_NAME t, ${MContract.TABLE_NAME} c WHERE t.is_enabled AND c.is_active AND t.contract_id = c.id AND (date_last IS NULL OR date_last + tinterval < now())")
      .as(rowParser *)
  }
}


final case class MTariffFee(
  id          : Option[Int] = None,
  contractId  : Int,
  name        : String,
  ttype       : BTariffType = BTariffTypes.Fee,
  isEnabled   : Boolean,
  dateFirst   : DateTime,
  dateCreated : DateTime = DateTime.now,
  dateModified: Option[DateTime] = None,
  dateLast    : Option[DateTime] = None,
  tinterval   : PGInterval,
  dateStatus  : DateTime = DateTime.now,
  generation  : Int = 0,
  debitCount  : Int = 0,
  fee         : Float,
  feeCC       : String = "RUB"
) extends SqlModelSave with MContractSel with SqlModelDelete with MTariff {
  import MTariffFee._

  override type T = MTariffFee
  override def companion = MTariffFee

  def tintervalPretty: String = FormUtil.pgIntervalPretty(tinterval)

  /** Доступен ли ключ ряда в текущем инстансе? */
  override def hasId: Boolean = id.isDefined

  override def saveInsert(implicit c: Connection): MTariffFee = {
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
