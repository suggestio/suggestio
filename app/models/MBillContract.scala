package models

import anorm._
import util.AnormJodaTime._
import util.AnormPgArray._
import org.joda.time.DateTime
import io.suggest.util.SioRandom.rnd
import java.sql.Connection
import util.SqlModelSave
import java.text.DecimalFormat
import org.joda.time.format.DateTimeFormat
import io.suggest.util.TextUtil
import play.api.Play.{current, configuration}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 18.04.14 9:51
 * Description: Биллинг: SQL-модель для работы со списком договоров.
 */
// TODO Модель синхронная. Надо бы её рассинхронизировать, когда в asynchbase переедут на netty 4.x.
object MBillContract extends SqlModelStatic[MBillContract] {
  import SqlParser._

  /** Комиссия s.io за размещение рекламной карточки. Пока что одна для всех. Вероятно, надо вынести это в другое место. */
  val SIO_COMISSION_SHARE_DFLT = configuration.getDouble("adv.comission.sio").map(_.toFloat) getOrElse 0.300000F

  private def idFormatter = new DecimalFormat("000")

  val TABLE_NAME: String = "bill_contract"

  val rowParser = get[Pk[Int]]("id") ~ get[Int]("crand") ~ get[String]("adn_id") ~ get[DateTime]("contract_date") ~
    get[DateTime]("date_created") ~ get[Option[String]]("hidden_info") ~ get[Boolean]("is_active") ~
    get[Option[String]]("suffix") ~ get[Float]("sio_comission") map {
    case id ~ crand ~ adnId ~ contractDate ~ dateCreated ~ hiddenInfo ~ isActive ~ suffix ~ sioComission =>
      MBillContract(
        id = id,  crand = crand,  adnId = adnId, contractDate = contractDate,
        dateCreated = dateCreated,  hiddenInfo = hiddenInfo,  isActive = isActive, suffix = suffix
      )
  }

  val CONTRACT_DATE_FMT = DateTimeFormat.forPattern("dd.MM.yyyy")

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

  /**
   * Найди все вхождения для списка ands.
   * @param adnIds Коллекция id узлов рекламной сети.
   * @param isActive Необязательный фильтр по значенияем в колонке is_active.
   * @return Список экземпляров [[MBillContract]] в неопределённом порядке.
   */
  def findForAdns(adnIds: Traversable[String], isActive: Option[Boolean] = None)(implicit c: Connection): List[MBillContract] = {
    val reqSql = new StringBuilder("SELECT * FROM ")
      .append(TABLE_NAME)
      .append(" WHERE adn_id = ANY({adnIds})")
    var args: List[NamedParameter] = List('adnIds -> strings2pgArray(adnIds))
    // Если задан isActive, то нужно добавить ещё проверку в исходный запрос
    if (isActive.isDefined) {
      reqSql.append(" AND is_active = {is_active}")
      args ::= (('is_active, isActive.get) : NamedParameter)
    }
    SQL(reqSql.toString())
      .on(args : _*)
      .as(rowParser *)
  }

  /**
   * Поиск по crand. Используется при опечатках в id контракта.
   * @param crand Рандомная константа контракта.
   * @return Список найденных с указанной константой.
   */
  def findByCrand(crand: Int)(implicit c: Connection): List[MBillContract] = {
    // TODO Добавить сортировку по similarity id и suffix
    SQL("SELECT * FROM " + TABLE_NAME + " WHERE crand = {crand}")
      .on('crand -> crand)
      .as(rowParser *)
  }

  val LEGAL_CONTRACT_ID_RE = "(?iu)(\\d{3,10})-(\\d{3})(/[a-zа-я\\d]{1,5})?".r

  /** Распарсить номера договоров из строки. */
  def parseLegalContractId(text: String): List[LegalContractId] = {
    LEGAL_CONTRACT_ID_RE.findAllIn(text)
      .foldLeft[List[LegalContractId]](Nil) { (acc, e) =>
        val LEGAL_CONTRACT_ID_RE(idStr, crandStr, suffixRaw) = e
        val suffix = if (suffixRaw == null || suffixRaw.isEmpty) {
          None
        } else {
          Some(suffixRaw.tail)
        }
        LegalContractId(idStr.toInt, crand = crandStr.toInt, suffix = suffix, raw = e) :: acc
      }
      .reverse
  }

  /** Визуально сравнить два суффикса, чтобы они совпадали. */
  def matchSuffixes(suffix0: Option[String], suffix1: Option[String]): Boolean = {
    (suffix0, suffix1) match {
      case (Some(_suffix0), Some(_suffix1)) =>
        val s1 = _suffix0.map(TextUtil.mischarFixRu)
        val s2 = _suffix1.map(TextUtil.mischarFixRu)
        s1 equalsIgnoreCase s2

      case _ => false
    }
  }

  /** Напечатать строкой номер договора на основе переданных идентификаторов. */
  def formatLegalContractId(id: Int, crand: Int, suffix: Option[String]): String = {
    val fmt = idFormatter
    val idStr = fmt format id
    val sb = new StringBuilder(idStr)
    // Нужно добавить нули в начале crand
    val crandStr = fmt format crand
    sb.append('-').append(crandStr)
    if (suffix.isDefined)
      sb.append('/').append(suffix.get)
    sb.toString()
  }

  case class LegalContractId(id: Int, crand: Int, suffix: Option[String], raw: String) {
    override def toString: String = {
      if (raw == null || raw.isEmpty)
        formatLegalContractId(id, crand=crand, suffix=suffix)
      else
        raw
    }

    def contractId = id
  }

  /**
   * Выдать все активные контракты.
   * @return Список контрактов в неопределённом порядке.
   */
  def findAllActive(implicit c: Connection): List[MBillContract] = {
    SQL("SELECT * FROM " + TABLE_NAME + " WHERE is_active")
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
  var sioComission: Float = MBillContract.SIO_COMISSION_SHARE_DFLT,
  id            : Pk[Int] = NotAssigned
) extends SqlModelSave[MBillContract] {

  def hasId: Boolean = id.isDefined

  def legalContractId = formatLegalContractId(id.get, crand=crand, suffix=suffix)

  def printContractDate: String = CONTRACT_DATE_FMT.print(contractDate)

  def suffixMatches(suffix1: Option[String]) = MBillContract.matchSuffixes(suffix, suffix1)

  /**
   * Добавить в базу текущую запись.
   * @return Новый экземпляр сабжа.
   */
  def saveInsert(implicit c: Connection): MBillContract = {
    SQL("INSERT INTO " + TABLE_NAME + "(adn_id, contract_date, date_created, hidden_info, is_active, crand, suffix, sio_comission)" +
        " VALUES ({adnId}, {contractDate}, {dateCreated}, {hiddenInfo}, {isActive}, {crand}, {suffix}, {sioComission})")
      .on('adnId -> adnId, 'contractDate -> contractDate, 'dateCreated -> dateCreated, 'hiddenInfo -> hiddenInfo,
          'isActive -> isActive, 'crand -> crand, 'suffix -> suffix, 'sioComission -> sioComission)
      .executeInsert(rowParser single)
  }


  /**
   * Обновлить в таблице текущую запись.
   * @return Кол-во обновлённых рядов. Обычно 0 либо 1.
   */
  def saveUpdate(implicit c: Connection): Int = {
    SQL("UPDATE " + TABLE_NAME + " SET contract_date = {contractDate}, hidden_info = {hiddenInfo}," +
        " is_active = {isActive}, suffix = {suffix}, sio_comission = {sioComission} WHERE id = {id}")
      .on('id -> id.get, 'contractDate -> contractDate, 'hiddenInfo -> hiddenInfo,
          'isActive -> isActive, 'suffix -> suffix, 'sioComission -> sioComission)
      .executeUpdate()
  }

}


trait MBillContractSel {
  def contractId: Int
  def contract(implicit c: Connection) = MBillContract.getById(contractId)
}


trait FindByContract[T] extends SqlModelStatic[T] {

  /**
   * Найти все ряды для указанного номера договора.
   * @param contractId id договора.
   * @return Список рядов в неопределённом порядке.
   */
  def findByContractId(contractId: Int, policy: SelectPolicy = SelectPolicies.NONE)(implicit c: Connection): List[T] = {
    findBy(" WHERE contract_id = {contractId}", policy, 'contractId -> contractId)
  }

  def findByContractIds(contractIds: Traversable[Int], policy: SelectPolicy = SelectPolicies.NONE)(implicit c: Connection): List[T] = {
    findBy(" WHERE contract_id = ANY({contractIds})", policy, 'contractIds -> seqInt2pgArray(contractIds))
  }

  /**
   * Найти все ряды, которые НЕ содержат указанный номер договора.
   * @param contractId номер договора.
   * @return Список рядов в неопределённом порядке.
   */
  def findByNotContractId(contractId: Int, policy: SelectPolicy = SelectPolicies.NONE)(implicit c: Connection): List[T] = {
    findBy(" WHERE contract_id != {contractId}", policy, 'contractId -> contractId)
  }

}


