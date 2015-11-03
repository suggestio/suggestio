package models.mbill

import java.sql.Connection
import java.text.DecimalFormat
import java.{util => ju}

import anorm._
import io.suggest.model.es.EsModelUtil.FieldsJsonAcc
import io.suggest.model.es.{EsModelUtil, ToPlayJsonObj}
import io.suggest.util.SioRandom.rnd
import io.suggest.util.TextUtil
import models._
import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat
import play.api.Play.{configuration, current}
import util.anorm.AnormJodaTime._
import util.anorm.AnormPgArray._
import util.event.{ContractDeletedEvent, SiowebNotifier}
import util.SqlModelSave
import play.api.libs.json._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 18.04.14 9:51
 * Description: Биллинг: SQL-модель для работы со списком договоров.
 */
object MContract extends SqlModelStatic with FromJson {
  import SqlParser._

  override type T = MContract

  private def idFormatter = new DecimalFormat("000")

  /** При создании контракта дефолтовое значение суффикса. */
  lazy val CONTRACT_SUFFIX_DFLT = configuration.getString("bill.contract.suffix.dflt") getOrElse "CEO"

  val ID_FN             = "id"
  val ADN_ID_FN         = "adn_id"
  val CRAND_FN          = "crand"
  val CONTRACT_DATE_FN  = "contract_date"
  val DATE_CREATED_FN   = "date_created"
  val HIDDEN_INFO_FN    = "hidden_info"
  val IS_ACTIVE_FN      = "is_active"
  val SUFFIX_FN         = "suffix"

  val TABLE_NAME = "bill_contract"

  val ADN_ID_PARSER = str(ADN_ID_FN)

  val rowParser = get[Option[Int]](ID_FN) ~ int(CRAND_FN) ~ ADN_ID_PARSER ~ get[DateTime](CONTRACT_DATE_FN) ~
    get[DateTime](DATE_CREATED_FN) ~ get[Option[String]](HIDDEN_INFO_FN) ~ bool(IS_ACTIVE_FN) ~
    get[Option[String]](SUFFIX_FN) map {
    case id ~ crand ~ adnId ~ contractDate ~ dateCreated ~ hiddenInfo ~ isActive ~ suffix =>
      MContract(
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
  def findForAdn(adnId: String, isActive: Option[Boolean] = None)(implicit c: Connection): List[MContract] = {
    val reqSql = new StringBuilder()
      .append("SELECT * FROM ").append(TABLE_NAME).append(" WHERE ").append(ADN_ID_FN).append(" = {adnId}")
    var args: List[NamedParameter] = List('adnId -> adnId)
    // Если задан isActive, то добавляем в запрос ещё кое-какие данные.
    if (isActive.isDefined) {
      reqSql.append(" AND ").append(IS_ACTIVE_FN).append(" = {is_active}")
      args ::= (('is_active, isActive.get) : NamedParameter)
    }
    // Добавляем сортировку и запускаем на исполнение.
    reqSql.append(" ORDER BY ").append(ID_FN).append(" ASC")
    SQL(reqSql.toString())
      .on(args : _*)
      .as(rowParser.*)
  }

  /**
   * Найди все вхождения для списка ands.
   * @param adnIds Коллекция id узлов рекламной сети.
   * @param isActive Необязательный фильтр по значенияем в колонке is_active.
   * @return Список экземпляров [[MContract]] в неопределённом порядке.
   */
  def findForAdns(adnIds: Traversable[String], isActive: Option[Boolean] = None)(implicit c: Connection): List[MContract] = {
    val reqSql = new StringBuilder("SELECT * FROM ")
      .append(TABLE_NAME)
      .append(" WHERE ").append(ADN_ID_FN).append(" = ANY({adnIds})")
    var args: List[NamedParameter] = List('adnIds -> strings2pgArray(adnIds))
    // Если задан isActive, то нужно добавить ещё проверку в исходный запрос
    if (isActive.isDefined) {
      reqSql.append(" AND ").append(IS_ACTIVE_FN).append(" = {is_active}")
      args ::= (('is_active, isActive.get) : NamedParameter)
    }
    SQL(reqSql.toString())
      .on(args : _*)
      .as(rowParser.*)
  }

  /**
   * Поиск по crand. Используется при опечатках в id контракта.
   * @param crand Рандомная константа контракта.
   * @return Список найденных с указанной константой.
   */
  def findByCrand(crand: Int)(implicit c: Connection): List[MContract] = {
    // TODO Добавить сортировку по similarity id и suffix
    SQL(s"SELECT * FROM $TABLE_NAME WHERE $CRAND_FN = {crand}")
      .on('crand -> crand)
      .as(rowParser.*)
  }

  val LEGAL_CONTRACT_ID_RE = "(?iu)(\\d{3,10})-(\\d{3})(/[-a-z_а-я\\d]{1,16})?".r

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
  def findAllActive(implicit c: Connection): List[MContract] = {
    SQL(s"SELECT * FROM $TABLE_NAME WHERE $IS_ACTIVE_FN")
      .as(rowParser.*)
  }

  def hasActiveForNode(adnId: String)(implicit c: Connection): Boolean = {
    SQL(s"SELECT count(*) > 0 AS bool FROM $TABLE_NAME WHERE $ADN_ID_FN = {adnId} AND $IS_ACTIVE_FN")
      .on('adnId -> adnId)
      .as(SqlModelStatic.boolColumnParser.single)
  }

  /** Десериализация из json для нужд [[MInviteRequest]]. */
  val fromJson: PartialFunction[Any, MContract] = {
    case jmap: ju.Map[_,_] =>
      import EsModelUtil.{booleanParser, dateTimeParser, intParser, stringParser}
      MContract(
        adnId         = stringParser(jmap get ADN_ID_FN),
        contractDate  = dateTimeParser(jmap get CONTRACT_DATE_FN),
        suffix        = Option(jmap get SUFFIX_FN) map stringParser,
        dateCreated   = Option(jmap get DATE_CREATED_FN).fold(DateTime.now)(dateTimeParser),
        hiddenInfo    = Option(jmap get HIDDEN_INFO_FN) map stringParser,
        isActive      = Option(jmap get IS_ACTIVE_FN).fold(true)(booleanParser),
        crand         = intParser(jmap get CRAND_FN),
        id            = Option(jmap get ID_FN) map intParser
      )
  }

  override def deleteById(id: Int)(implicit c: Connection): Int = {
    getById(id, SelectPolicies.UPDATE) match {
      case Some(mbc)  => _deleteMbc(mbc)
      case None       => 0
    }
  }

  private def _deleteMbc(mbc: MContract)(implicit c: Connection): Int = {
    mbc.id.fold(0) { mbcId =>
      val rowsDeleted = super.deleteById(mbcId)
      if (rowsDeleted > 0)
        SiowebNotifier.publish(ContractDeletedEvent(mbcId))
      rowsDeleted
    }
  }

  /** Удалить контракты все контракты для указанного узла.
    * @param adnId id узла.
    * @return Кол-во удалённых рядов.
    */
  def deleteByAdnId(adnId: String)(implicit c: Connection): Int = {
    findForAdn(adnId)
      .foldLeft(0) { (counter, mbc)  =>  _deleteMbc(mbc) + counter }
  }


  /** Псевдослучайное число от 101 до 999. Избегаем нулей, чтобы не путали с буквой 'O'. */
  def crand(): Int = {
    rnd.nextInt(898) + 101
  }

}

import models.mbill.MContract._

final case class MContract(
  adnId         : String,
  contractDate  : DateTime        = DateTime.now,
  suffix        : Option[String]  = Some(MContract.CONTRACT_SUFFIX_DFLT),
  dateCreated   : DateTime        = DateTime.now,
  hiddenInfo    : Option[String]  = None,
  isActive      : Boolean         = true,
  crand         : Int             = MContract.crand(),
  id            : Option[Int]     = None
) extends SqlModelSave with ToPlayJsonObj with SqlModelDelete {

  def hasId: Boolean = id.isDefined
  def legalContractId = formatLegalContractId(id.get, crand=crand, suffix=suffix)

  def printContractDate: String = CONTRACT_DATE_FMT.print(contractDate)

  override def companion = MContract
  override type T = MContract

  def suffixMatches(suffix1: Option[String]) = MContract.matchSuffixes(suffix, suffix1)

  /**
   * Добавить в базу текущую запись.
   * @return Новый экземпляр сабжа.
   */
  def saveInsert(implicit c: Connection): T = {
    SQL(s"INSERT INTO $TABLE_NAME ($ADN_ID_FN, $CONTRACT_DATE_FN, $DATE_CREATED_FN, $HIDDEN_INFO_FN, $IS_ACTIVE_FN, $CRAND_FN, $SUFFIX_FN)" +
        " VALUES ({adnId}, {contractDate}, {dateCreated}, {hiddenInfo}, {isActive}, {crand}, {suffix})")
      .on('adnId -> adnId, 'contractDate -> contractDate, 'dateCreated -> dateCreated, 'hiddenInfo -> hiddenInfo,
          'isActive -> isActive, 'crand -> crand, 'suffix -> suffix)
      .executeInsert(rowParser.single)
  }


  /**
   * Обновлить в таблице текущую запись.
   * @return Кол-во обновлённых рядов. Обычно 0 либо 1.
   */
  def saveUpdate(implicit c: Connection): Int = {
    SQL(s"UPDATE $TABLE_NAME SET $CONTRACT_DATE_FN = {contractDate}, $HIDDEN_INFO_FN = {hiddenInfo}," +
        s" $IS_ACTIVE_FN = {isActive}, $SUFFIX_FN = {suffix} WHERE $ID_FN = {id}")
      .on('id -> id.get, 'contractDate -> contractDate, 'hiddenInfo -> hiddenInfo,
          'isActive -> isActive, 'suffix -> suffix)
      .executeUpdate()
  }

  /** Сериализация в JSON экземпляра этого класса для нужд [[MInviteRequest]]. */
  def toPlayJsonAcc: FieldsJsonAcc = {
    var acc: FieldsJsonAcc = List(
      ADN_ID_FN         -> JsString(adnId),
      CONTRACT_DATE_FN  -> EsModelUtil.date2JsStr(contractDate),
      DATE_CREATED_FN   -> EsModelUtil.date2JsStr(dateCreated),
      IS_ACTIVE_FN      -> JsBoolean(isActive),
      CRAND_FN          -> JsNumber(crand)
    )
    if (suffix.isDefined)
      acc ::= SUFFIX_FN -> JsString(suffix.get)
    if (hiddenInfo.isDefined)
      acc ::= HIDDEN_INFO_FN -> JsString(hiddenInfo.get)
    acc
  }

  override def toPlayJsonWithId: JsObject = {
    var acc = toPlayJsonAcc
    if (id.isDefined)
      acc ::= ID_FN -> JsNumber(id.get)
    JsObject(acc)
  }

  override def delete(implicit c: Connection): Int = {
    MContract._deleteMbc(this)
  }
}


trait MContractSel {
  def contractId: Int
  def contract(implicit c: Connection) = MContract.getById(contractId)
}


trait FindByContract extends SqlModelStatic {

  /**
   * Найти все ряды для указанного номера договора.
   * @param contractId id договора.
   * @return Список рядов в неопределённом порядке.
   */
  def findByContractId(contractId: Int, policy: SelectPolicy = SelectPolicies.NONE)(implicit c: Connection): List[T] = {
    findBy(" WHERE contract_id = {contractId}", policy, 'contractId -> contractId)
  }

  /**
   * Прочитать один ряд для указанного контракта. Если рядов несколько, то выбрать тот, у которого максимальный id.
   * @param contractId id контракта.
   * @return Some(T) или None, если подходящих рядов вообще нет.
   */
  def getLatestForContractId(contractId: Int)(implicit c: Connection): Option[T] = {
    SQL(s"SELECT * FROM $TABLE_NAME WHERE contract_id = {contractId} ORDER BY id DESC LIMIT 1")
      .on('contractId -> contractId)
      .as(rowParser.*)
      .headOption
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

  /**
   * Найти все ряды в таблице, которые относятся к указанному узлу
   * @param adnId id узла, прописанного к договоре.
   * @return Список результатов в неопределённом порядке.
   */
  def findByContractAdnId(adnId: String)(implicit c: Connection): List[T] = {
    SQL(s"SELECT t.* FROM $TABLE_NAME t WHERE t.contract_id IN (SELECT id FROM bill_contract WHERE adn_id = {adnId} AND is_active)")
      .on('adnId -> adnId)
      .as(rowParser.*)
  }

  /**
    * Есть ли ряды, содержащие contract_id, контракт которого относится к указанному узлу.
    * @param adnId id узла.
    * @return true, если есть ряды, чей контракт активен и относится к указанному узлу.
    */
  def hasByContractAdnId(adnId: String)(implicit c: Connection): Boolean = {
    SQL("SELECT count(t.*) > 0 AS bool FROM " + TABLE_NAME + " t WHERE t.contract_id IN (SELECT id FROM bill_contract WHERE adn_id = {adnId} AND is_active) LIMIT 1")
      .on('adnId -> adnId)
      .as(SqlModelStatic.boolColumnParser.single)
  }

}
