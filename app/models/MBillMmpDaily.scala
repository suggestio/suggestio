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
    get[Float]("mmp_primetime") ~ get[String]("currency_code") ~ get[Float]("on_start_page") ~
    get[String]("weekend_cal_id") ~ get[String]("prime_cal_id") ~ get[Float]("on_rcvr_cat") map {
    case id ~ contractId ~ mmpWeekday ~ mmpWeekend ~ mmpPrimetime ~ currencyCode ~ onStartPage ~ weekendCalId ~ primeCalId ~ onRcvrCat =>
      MBillMmpDaily(
        id = id, contractId = contractId, currencyCode = currencyCode,
        mmpWeekday = mmpWeekday, mmpWeekend = mmpWeekend, mmpPrimetime = mmpPrimetime,
        onRcvrCat = onRcvrCat, onStartPage = onStartPage, weekendCalId = weekendCalId, primeCalId = primeCalId
      )
  }

  /** Найти все adnId через таблицу контрактов.
    * @return Список adnId без повторений в неопределённом порядке.
    */
  def findAllAdnIds(implicit c: Connection): List[String] = {
    SQL(s"SELECT DISTINCT mbc.adn_id FROM ${MBillContract.TABLE_NAME} mbc WHERE mbc.id IN (SELECT DISTINCT contract_id FROM $TABLE_NAME)")
      .as(MBillContract.ADN_ID_PARSER *)
  }

  /** Найти все ряды, в которых встречается указанный календарь. */
  def findForCalId(calId: String)(implicit c: Connection): List[MBillMmpDaily] = {
    SQL("SELECT * FROM " + TABLE_NAME + " WHERE weekend_cal_id = {calId} OR prime_cal_id = {calId}")
      .on('calId -> calId)
      .as(rowParser *)
  }
}


import MBillMmpDaily._


case class MBillMmpDaily(
  contractId    : Int,
  mmpWeekday    : Float,
  mmpWeekend    : Float,
  mmpPrimetime  : Float,
  onRcvrCat     : Float,
  onStartPage   : Float,
  weekendCalId  : String,
  primeCalId    : String,
  currencyCode  : String = CurrencyCodeOpt.CURRENCY_CODE_DFLT,
  id            : Pk[Int] = NotAssigned
) extends SqlModelSave[MBillMmpDaily] with CurrencyCode with MBillContractSel with SqlModelDelete {

  override def hasId = id.isDefined
  override def companion = MBillMmpDaily

  override def saveInsert(implicit c: Connection): MBillMmpDaily = {
    SQL("INSERT INTO " + TABLE_NAME + "(contract_id, currency_code, mmp_weekday, mmp_weekend, mmp_primetime, on_start_page, weekend_cal_id, prime_cal_id, on_rcvr_cat) " +
      "VALUES ({contractId}, {currencyCode}, {mmpWeekday}, {mmpWeekend}, {mmpPrimetime}, {onStartPage}, {weekendCalId}, {primeCalId}, {onRcvrCat})")
      .on('contractId -> contractId, 'currencyCode -> currencyCode, 'mmpWeekday -> mmpWeekday, 'mmpWeekend -> mmpWeekend,
          'mmpPrimetime -> mmpPrimetime, 'onStartPage -> onStartPage, 'weekendCalId -> weekendCalId, 'primeCalId -> primeCalId,
          'onRcvrCat -> onRcvrCat)
      .executeInsert(rowParser single)
  }

  override def saveUpdate(implicit c: Connection): Int = {
    SQL("UPDATE " + TABLE_NAME + " SET currency_code = {currencyCode}, mmp_weekday = {mmpWeekday}, " +
      "mmp_weekend = {mmpWeekend}, mmp_primetime = {mmpPrimetime}, on_start_page = {onStartPage}, on_rcvr_cat = {onRcvrCat}, " +
      "weekend_cal_id = {weekendCalId}, prime_cal_id = {primeCalId} WHERE id = {id}")
      .on('id -> id.get, 'currencyCode -> currencyCode, 'mmpWeekday -> mmpWeekday, 'mmpWeekend -> mmpWeekend,
          'onRcvrCat -> onRcvrCat, 'mmpPrimetime -> mmpPrimetime, 'onStartPage -> onStartPage,
          'weekendCalId -> weekendCalId, 'primeCalId -> primeCalId)
      .executeUpdate()
  }
}
