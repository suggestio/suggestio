package models

import anorm._
import io.suggest.model.EsModel.FieldsJsonAcc
import io.suggest.model.ToPlayJsonObj
import util.SqlModelSave
import java.sql.Connection
import util.AnormPgArray._
import play.api.libs.json._
import java.{util => ju}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 18.04.14 11:07
 * Description: Балансы на счетах узлов рекламной сети. Как бы "кошельки" рекламных узлов.
 */
object MBillBalance extends SqlModelStaticMinimal with FromJson {
  import SqlParser._

  override type T = MBillBalance

  val TABLE_NAME: String = "bill_balance"

  val CURRENCY_CODE_DFLT = "RUB"

  val ADN_ID_FN     = "adn_id"
  val AMOUNT_FN     = "amount"
  val CURRENCY_FN   = "currency"
  val OVERDRAFT_FN  = "overdraft"
  val BLOCKED_FN    = "blocked"

  val rowParser = get[String](ADN_ID_FN) ~ get[Float](AMOUNT_FN) ~ get[Option[String]](CURRENCY_FN) ~
    get[Float](OVERDRAFT_FN) ~ get[Float](BLOCKED_FN) map {
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
      .append(" WHERE ").append(ADN_ID_FN).append(" = {adnId}")
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
      .append(" WHERE ").append(ADN_ID_FN).append(" = ANY({adnIds})")
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
    SQL(s"UPDATE $TABLE_NAME SET $AMOUNT_FN = $AMOUNT_FN + {addAmount} WHERE $ADN_ID_FN = {adnId}")
      .on('adnId -> adnId, 'addAmount -> addAmount)
      .executeUpdate()
  }

  /**
   * Заблокировать часть денег на счете. По сути, ускоренная версия комбинации updateAmount() и updateBlocked().
   * @param adnId id узла (id владельца кошелька).
   * @param amount Блокируемый объём средств.
   * @return Кол-во обновлённых рядов, т.е. 0 или 1.
   */
  def blockAmount(adnId: String, amount: Float)(implicit c: Connection): Int = {
    SQL(s"UPDATE $TABLE_NAME SET $AMOUNT_FN = $AMOUNT_FN - {amount}, $BLOCKED_FN = $BLOCKED_FN + {amount} WHERE $ADN_ID_FN = {adnId}")
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
    SQL(s"UPDATE $TABLE_NAME SET $BLOCKED_FN = $BLOCKED_FN + {amount} WHERE $ADN_ID_FN = {adnId}")
      .on('adnId -> adnId, 'amount -> blockAmount)
      .executeUpdate()
  }

  def hasForNode(adnId: String)(implicit c: Connection): Boolean = {
    SQL(s"SELECT count(*) > 0 AS bool FROM $TABLE_NAME WHERE $ADN_ID_FN = {adnId}")
      .on('adnId -> adnId)
      .as(SqlModelStatic.boolColumnParser single)
  }

  /** Десериализатор экземпляра модели из json-представления. */
  val fromJson: PartialFunction[Any, MBillBalance] = {
    case jmap: ju.Map[_,_] =>
      import io.suggest.model.EsModel.{stringParser, floatParser}
      MBillBalance(
        adnId     = stringParser(jmap get ADN_ID_FN),
        amount    = floatParser(jmap get AMOUNT_FN),
        currencyCodeOpt = Option(jmap get CURRENCY_FN) map stringParser,
        overdraft = floatParser(jmap get OVERDRAFT_FN),
        blocked   = floatParser(jmap get BLOCKED_FN)
      )
  }

}


import MBillBalance._

case class MBillBalance(
  adnId: String,
  amount: Float,
  currencyCodeOpt: Option[String] = Some(CurrencyCodeOpt.CURRENCY_CODE_DFLT),
  overdraft: Float = 0F,
  blocked: Float = 0F
) extends SqlModelSave[MBillBalance] with CurrencyCodeOpt with ToPlayJsonObj {

  def hasId: Boolean = true

  /** Добавить в базу текущую запись.
    * @return Новый экземпляр сабжа.
    */
  def saveInsert(implicit c: Connection): MBillBalance = {
    SQL(s"INSERT INTO $TABLE_NAME ($ADN_ID_FN, $AMOUNT_FN, $CURRENCY_FN, $BLOCKED_FN)" +
        " VALUES({adnId}, {amount}, {currencyCode}, {blocked})")
      .on('adnId -> adnId, 'amount -> amount, 'currencyCode -> currencyCodeOpt, 'blocked -> blocked)
      .executeInsert(rowParser single)
  }

  /** Обновлить в таблице текущую запись.
    * @return Кол-во обновлённых рядов. Обычно 0 либо 1.
    */
  def saveUpdate(implicit c: Connection): Int = {
    SQL(s"UPDATE $TABLE_NAME SET $AMOUNT_FN = {amount}, $OVERDRAFT_FN = {overdraft}, $BLOCKED_FN = {blocked} WHERE $ADN_ID_FN = {adnId}")
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

  /** Атомарно обновить заблокированную и текущую сумму. */
  def updateBlocked(blockAmount: Float)(implicit c: Connection): Int = {
    MBillBalance.blockAmount(adnId, blockAmount)
  }

  /** Сериализация в play JSON. Для нужд [[MInviteRequest]]. */
  override def toPlayJsonAcc: FieldsJsonAcc = {
    var acc: FieldsJsonAcc = List(
      ADN_ID_FN     -> JsString(adnId),
      AMOUNT_FN     -> JsNumber(amount),
      OVERDRAFT_FN  -> JsNumber(overdraft),
      BLOCKED_FN    -> JsNumber(blocked)
    )
    if (currencyCodeOpt.isDefined)
      acc ::= CURRENCY_FN -> JsString(currencyCodeOpt.get)
    acc
  }
  override def toPlayJsonWithId: JsObject = toPlayJson
}
