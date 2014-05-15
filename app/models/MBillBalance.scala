package models

import anorm._
import util.SqlModelSave
import java.sql.Connection
import util.AnormPgArray._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 18.04.14 11:07
 * Description: Балансы на счетах узлов рекламной сети. Как бы "кошельки" рекламных узлов.
 */
object MBillBalance {
  import SqlParser._

  val TABLE_NAME: String = "bill_balance"

  val CURRENCY_CODE_DFLT = "RUB"

  val rowParser = get[String]("adn_id") ~ get[Float]("amount") ~ get[Option[String]]("currency") ~ get[Float]("overdraft") map {
    case adnId ~ amount ~ currencyCodeOpt ~ overdraft =>
      MBillBalance(
        adnId = adnId,
        amount = amount,
        currencyCodeOpt = currencyCodeOpt,
        overdraft = overdraft
      )
  }

  /**
   * Прочитать ряд по ключу.
   * @param adnId id узла сети.
   * @return
   */
  def getByAdnId(adnId: String, policy: SelectPolicy = SelectPolicies.NONE)(implicit c: Connection): Option[MBillBalance] = {
    // Собрать реквест с учётом возможного наличия заданной политики селекта ряда.
    val req = new StringBuilder(64, "SELECT * FROM ")
      .append(TABLE_NAME)
      .append(" WHERE adn_id = {adnId}")
    policy.append2sb(req)
    SQL(req.toString())
      .on('adnId -> adnId)
      .as(rowParser *)
      .headOption
  }


  /**
   * Получение балансов для указанных id узлов рекламной сети.
   * @param adnIds Коллекция из id узлов рекламной сети.
   * @param policy Политика локов в SELECT.
   * @return Список [[MBillBalance]] в неопределённом порядке.
   */
  def getByAdnIds(adnIds: Traversable[String], policy: SelectPolicy = SelectPolicies.NONE)(implicit c: Connection): List[MBillBalance] = {
    val req = new StringBuilder(64, "SELECT * FROM ")
      .append(TABLE_NAME)
      .append(" WHERE adn_id = ANY({adnIds})")
    policy.append2sb(req)
    SQL(req.toString())
      .on('adnIds -> strings2pgArray(adnIds))
      .as(rowParser *)
  }


  /**
   * Заапдейтить поле amount.
   * @param adnId id кошелька (узла рекламной сети).
   * @param addAmount Изменение баланса.
   * @return Кол-во обновлённых рядов.
   */
  def updateAmount(adnId: String, addAmount: Float)(implicit c: Connection): Int = {
    SQL("UPDATE " + TABLE_NAME + " SET amount = amount + {addAmount} WHERE adn_id = {adnId}")
      .on('adnId -> adnId, 'addAmount -> addAmount)
      .executeUpdate()
  }

}


import MBillBalance._

case class MBillBalance(
  adnId: String,
  amount: Float,
  currencyCodeOpt: Option[String] = None,
  var overdraft: Float = 0F
) extends SqlModelSave[MBillBalance] with CurrencyCodeOpt {

  def hasId: Boolean = true

  /** Добавить в базу текущую запись.
    * @return Новый экземпляр сабжа.
    */
  def saveInsert(implicit c: Connection): MBillBalance = {
    SQL("INSERT INTO " + TABLE_NAME + "(adn_id, amount, currency)" +
        " VALUES({adnId}, {amount}, {currencyCode})")
      .on('adnId -> adnId, 'amount -> amount, 'currencyCode -> currencyCodeOpt)
      .executeInsert(rowParser single)
  }

  /** Обновлить в таблице текущую запись.
    * @return Кол-во обновлённых рядов. Обычно 0 либо 1.
    */
  def saveUpdate(implicit c: Connection): Int = {
    SQL("UPDATE " + TABLE_NAME + " SET amount = {amount}, overdraft = {overdraft} WHERE adn_id = {adnId}")
      .on('adnId -> adnId, 'amount -> amount, 'overdraft -> overdraft)
      .executeUpdate()
  }

  /**
   * Добавить на баланс указанный объём денег без учета валюты.
   * @param addAmount Изменение баланса.
   * @return Новый/этот инстанс [[MBillBalance]].
   */
  def updateAmount(addAmount: Float)(implicit c: Connection): MBillBalance = {
    val mbb1 = MBillBalance(adnId, amount  + addAmount, currencyCodeOpt)
    MBillBalance.updateAmount(adnId, addAmount) match {
      case 0 => mbb1.save
      case 1 => mbb1
    }
  }
}
