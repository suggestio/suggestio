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
    get[Float]("mmp_primetime") ~ get[String]("currency_code") ~ get[Float]("on_start_page") map {
    case id ~ contractId ~ mmpWeekday ~ mmpWeekend ~ mmpPrimetime ~ currencyCode ~ onStartPage =>
      MBillMmpDaily(
        id = id, contractId = contractId, currencyCode = currencyCode,
        mmpWeekday = mmpWeekday, mmpWeekend = mmpWeekend, mmpPrimetime = mmpPrimetime,
        onStartPage = onStartPage
      )
  }

}


import MBillMmpDaily._


case class MBillMmpDaily(
  contractId    : Int,
  mmpWeekday    : Float,
  mmpWeekend    : Float,
  mmpPrimetime  : Float,
  onStartPage   : Float,
  currencyCode  : String = CurrencyCodeOpt.CURRENCY_CODE_DFLT,
  id            : Pk[Int] = NotAssigned
) extends SqlModelSave[MBillMmpDaily] with CurrencyCode with MBillContractSel with SqlModelDelete {

  override def hasId = id.isDefined
  override def companion = MBillMmpDaily

  override def saveInsert(implicit c: Connection): MBillMmpDaily = {
    SQL("INSERT INTO " + TABLE_NAME + "(contract_id, currency_code, mmp_weekday, mmp_weekend, mmp_primetime, on_start_page) " +
      "VALUES ({contractId}, {currencyCode}, {mmpWeekday}, {mmpWeekend}, {mmpPrimetime}, {onStartPage})")
      .on('contractId -> contractId, 'currencyCode -> currencyCode, 'mmpWeekday -> mmpWeekday, 'mmpWeekend -> mmpWeekend,
          'mmpPrimetime -> mmpPrimetime, 'onStartPage -> onStartPage)
      .executeInsert(rowParser single)
  }

  override def saveUpdate(implicit c: Connection): Int = {
    SQL("UPDATE " + TABLE_NAME + " SET currency_code = {currencyCode}, mmp_weekday = {mmpWeekday}, " +
      "mmp_weekend = {mmpWeekend}, mmp_primetime = {mmpPrimetime}, on_start_page = {onStartPage} WHERE id = {id}")
      .on('id -> id.get, 'currencyCode -> currencyCode, 'mmpWeekday -> mmpWeekday, 'mmpWeekend -> mmpWeekend,
          'mmpPrimetime -> mmpPrimetime, 'onStartPage -> onStartPage)
      .executeUpdate()
  }
}
