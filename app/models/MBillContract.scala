package models

import anorm._
import util.AnormJodaTime._
import org.joda.time.DateTime
import io.suggest.util.SioRandom.rnd
import java.sql.Connection
import util.SqlModelSave
import java.text.DecimalFormat

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 18.04.14 9:51
 * Description: Биллинг: SQL-модель для работы со списком договоров.
 */
// TODO Модель синхронная. Надо бы её рассинхронизировать, когда в asynchbase переедут на netty 4.x.
object MBillContract extends SiowebSqlModelStatic[MBillContract] {
  import SqlParser._

  private val ID_FORMATTER = new DecimalFormat("000")

  val TABLE_NAME: String = "contract"

  val rowParser = get[Pk[Int]]("id") ~ get[Int]("crand") ~ get[String]("adn_id") ~ get[DateTime]("contract_date") ~
                  get[DateTime]("date_created") ~ get[Option[String]]("hidden_info") ~ get[Boolean]("is_active") ~
                  get[Option[String]]("suffix") map {
    case id ~ crand ~ adnId ~ contractDate ~ dateCreated ~ hiddenInfo ~ isActive ~ suffix =>
      MBillContract(
        id = id,  crand = crand,  adnId = adnId, contractDate = contractDate,
        dateCreated = dateCreated,  hiddenInfo = hiddenInfo,  isActive = isActive, suffix = suffix
      )
  }

  /**
   * Поиск по adnId с фильтрацией по id_active.
   * @param adnId es id узла рекламной сети.
   * @param isActive Опциональная фильтрация по isActive.
   * @return Список контрактов в порядке создания.
   */
  def findForAdn(adnId: String, isActive: Option[Boolean] = None)(implicit c: Connection): List[MBillContract] = {
    val reqSql = new StringBuilder()
      .append("SELECT * FROM ").append(TABLE_NAME).append(" WHERE adn_id = {adnId}")
    var args: List[NamedParameter] = List('adnId -> adnId)
    // Если задан isActive, то добавляем в запрос ещё кое-какие данные.
    if (isActive.isDefined) {
      reqSql.append(" AND is_active = {is_active}")
      args ::= (('is_active, isActive.get) : NamedParameter)
    }
    // Добавляем сортировку и запускаем на исполнение.
    reqSql.append(" ORDER BY id ASC")
    SQL(reqSql.toString())
      .on(args : _*)
      .as(rowParser *)
  }

}

import MBillContract._

case class MBillContract(
  adnId         : String,
  var contractDate: DateTime,
  var suffix    : Option[String] = None,
  dateCreated   : DateTime = DateTime.now,
  var hiddenInfo: Option[String] = None,
  var isActive  : Boolean = true,
  crand         : Int = rnd.nextInt(999) + 1, // от 1 до 999. Чтоб не было 0, а то перепутают с 'O'.
  id            : Pk[Int] = NotAssigned
) extends SqlModelSave[MBillContract] {

  def hasId: Boolean = id.isDefined

  lazy val legalContractId: String = {
    val idFmt = ID_FORMATTER.format(id.get)
    val sb = new StringBuilder(idFmt)
    sb.append('-').append(crand)
    if (suffix.isDefined)
      sb.append('/').append(suffix.get)
    sb.toString()
  }

  /**
   * Добавить в базу текущую запись.
   * @return Новый экземпляр сабжа.
   */
  def saveInsert(implicit c: Connection): MBillContract = {
    SQL("INSERT INTO " + TABLE_NAME + "(adn_id, contract_date, date_created, hidden_info, is_active, crand, suffix)" +
        " VALUES ({adnId}, {contractDate}, {dateCreated}, {hiddenInfo}, {isActive}, {crand}, {suffix})")
      .on('adnId -> adnId, 'contractDate -> contractDate, 'dateCreated -> dateCreated,
          'hiddenInfo -> hiddenInfo, 'isActive -> isActive, 'crand -> crand, 'suffix -> suffix)
      .executeInsert(rowParser single)
  }


  /**
   * Обновлить в таблице текущую запись.
   * @return Кол-во обновлённых рядов. Обычно 0 либо 1.
   */
  def saveUpdate(implicit c: Connection): Int = {
    SQL("UPDATE " + TABLE_NAME + " SET contract_date = {contractDate}, hidden_info = {hiddenInfo}," +
        " is_active = {isActive}, suffix = {suffix} WHERE id = {id}")
      .on('id -> id.get, 'contractDate -> contractDate, 'hiddenInfo -> hiddenInfo,
          'isActive -> isActive, 'suffix -> suffix)
      .executeUpdate()
  }

}
