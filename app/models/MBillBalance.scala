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

  val rowParser = get[String]("adn_id") ~ get[Float]("amount") ~ get[Option[String]]("currency") ~
    get[Float]("overdraft") ~ get[Float]("blocked") map {
    case adnId ~ amount ~ currencyCodeOpt ~ overdraft ~ blocked =>
      MBillBalance(
        adnId = adnId,
        amount = amount,
        currencyCodeOpt = currencyCodeOpt,
        overdraft = overdraft,
        blocked = blocked
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
   * @return Кол-во обновлённых рядов, т.е. 0 или 1.
   */
  def updateAmount(adnId: String, addAmount: Float)(implicit c: Connection): Int = {
    SQL("UPDATE " + TABLE_NAME + " SET amount = amount + {addAmount} WHERE adn_id = {adnId}")
      .on('adnId -> adnId, 'addAmount -> addAmount)
      .executeUpdate()
  }

  /**
   * Заблокировать часть денег на счете. По сути, ускоренная версия комбинации [[updateAmount()]] и [[updateBlocked()]].
   * @param adnId id узла (id владельца кошелька).
   * @param amount Блокируемый объём средств.
   * @return Кол-во обновлённых рядов, т.е. 0 или 1.
   */
  def blockAmount(adnId: String, amount: Float)(implicit c: Connection): Int = {
    SQL("UPDATE " + TABLE_NAME + " SET amount = amount - {amount}, blocked = blocked + {amount} WHERE adn_id = {adnId}")
      .on('adnId -> adnId, 'amount -> amount)
      .executeUpdate()
  }

  /**
   * Списать ранеее заблокированные средства.
   * @param adnId id узла (id владельца кошелька).
   * @param blockAmount Объём движения средств.
   * @return Кол-во обновлённых рядов, т.е. 0 или 1.
   */
  def updateBlocked(adnId: String, blockAmount: Float)(implicit c: Connection): Int = {
    SQL("UPDATE " + TABLE_NAME + " SET blocked = blocked + {amount} WHERE adn_id = {adnId}")
      .on('adnId -> adnId, 'amount -> blockAmount)
      .executeUpdate()
  }

  def hasForNode(adnId: String)(implicit c: Connection): Boolean = {
    SQL("SELECT count(*) > 0 AS bool FROM " + TABLE_NAME + " WHERE adn_id = {adnId}")
      .on('adnId -> adnId)
      .as(SqlModelStatic.boolColumnParser single)
  }
}


import MBillBalance._

case class MBillBalance(
  adnId: String,
  amount: Float,
  currencyCodeOpt: Option[String] = None,
  overdraft: Float = 0F,
  blocked: Float = 0F
) extends SqlModelSave[MBillBalance] with CurrencyCodeOpt {

  def hasId: Boolean = true

  /** Добавить в базу текущую запись.
    * @return Новый экземпляр сабжа.
    */
  def saveInsert(implicit c: Connection): MBillBalance = {
    SQL("INSERT INTO " + TABLE_NAME + "(adn_id, amount, currency, blocked)" +
        " VALUES({adnId}, {amount}, {currencyCode}, {blocked})")
      .on('adnId -> adnId, 'amount -> amount, 'currencyCode -> currencyCodeOpt, 'blocked -> blocked)
      .executeInsert(rowParser single)
  }

  /** Обновлить в таблице текущую запись.
    * @return Кол-во обновлённых рядов. Обычно 0 либо 1.
    */
  def saveUpdate(implicit c: Connection): Int = {
    SQL("UPDATE " + TABLE_NAME + " SET amount = {amount}, overdraft = {overdraft}, blocked = {blocked} WHERE adn_id = {adnId}")
      .on('adnId -> adnId, 'amount -> amount, 'overdraft -> overdraft, 'blocked -> blocked)
      .executeUpdate()
  }

  /**
   * Добавить на баланс указанный объём денег без учета валюты.
   * @param addAmount Изменение баланса.
   * @return Новый/этот инстанс [[MBillBalance]].
   */
  def updateAmount(addAmount: Float)(implicit c: Connection): MBillBalance = {
    val mbb1 = copy(amount = amount + addAmount)
    MBillBalance.updateAmount(adnId, addAmount) match {
      case 0 => mbb1.save
      case 1 => mbb1
    }
  }
  
  
  def updateBlocked(blockAmount: Float)(implicit c: Connection): Int = {
    MBillBalance.blockAmount(adnId, blockAmount)
  }

}
