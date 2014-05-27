package models

import anorm._
import util.SqlModelSave
import java.sql.Connection

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 27.05.14 11:52
 * Description: Модель работы с тарификациями при посуточной оплате рекламных модулей.
 * mmp = minimal module price.
 */
object MBillMmpDaily extends FindByContract[MBillMmpDaily] {
  import SqlParser._

  val TABLE_NAME = "bill_mmp_daily"

  val rowParser = get[Pk[Int]]("id") ~ get[Int]("contract_id") ~ get[Float]("mmp_weekday") ~ get[Float]("mmp_weekend") ~
    get[Float]("mmp_primetime") ~ get[String]("currency_code") map {
    case id ~ contractId ~ mmpWeekday ~ mmpWeekend ~ mmpPrimetime ~ currencyCode =>
      MBillMmpDaily(
        id = id, contractId = contractId, currencyCode = currencyCode,
        mmpWeekday = mmpWeekday, mmpWeekend = mmpWeekend, mmpPrimetime = mmpPrimetime
      )
  }

}


import MBillMmpDaily._


case class MBillMmpDaily(
  contractId    : Int,
  mmpWeekday    : Float,
  mmpWeekend    : Float,
  mmpPrimetime  : Float,
  currencyCode  : String = CurrencyCodeOpt.CURRENCY_CODE_DFLT,
  id            : Pk[Int] = NotAssigned
) extends SqlModelSave[MBillMmpDaily] with CurrencyCode with MBillContractSel {

  override def hasId = id.isDefined

  override def saveInsert(implicit c: Connection): MBillMmpDaily = {
    SQL("INSERT INTO " + TABLE_NAME + "(contract_id, currency_code, mmp_weekday, mmp_weekend, mmp_primetime) " +
      "VALUES ({contractId}, {currencyCode}, {mmpWeekday}, {mmpWeekend}, {mmpPrimetime})")
      .on('contractId -> contractId, 'currencyCode -> currencyCode, 'mmpWeekday -> mmpWeekday, 'mmpWeekend -> mmpWeekend,
          'mmpPrimetime -> mmpPrimetime)
      .executeInsert(rowParser single)
  }

  override def saveUpdate(implicit c: Connection): Int = {
    SQL("UPDATE " + TABLE_NAME + " SET currency_code = {currencyCode}, mmp_weekday = {mmpWeekday}, " +
      "mmp_weekend = {mmpWeekend}, mmp_primetime = {mmpPrimetime} WHERE id = {id}")
      .on('id -> id.get, 'currencyCode -> currencyCode, 'mmpWeekday -> mmpWeekday, 'mmpWeekend -> mmpWeekend,
          'mmpPrimetime -> mmpPrimetime)
      .executeUpdate()
  }
}
