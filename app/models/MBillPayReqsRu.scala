package models

import java.sql.Connection

import anorm._
import io.suggest.model.EsModel.FieldsJsonAcc
import io.suggest.model.ToPlayJsonObj
import util.SqlModelSave
import play.api.libs.json._
import java.{util => ju}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 11.06.14 14:59
 * Description: Каталог платежных реквизитов.
 */
object MBillPayReqsRu extends SqlModelStatic with FromJson {
  import SqlParser._

  override type T = MBillPayReqsRu

  override val TABLE_NAME = "bill_pay_reqs_ru"

  val ID_FN = "id"
  val CONTRACT_ID_FN = "contract_id"
  val R_NAME_FN = "r_name"
  val R_INN_FN = "r_inn"
  val R_KPP_FN = "r_kpp"
  val R_OKATO_FN = "r_okato"
  val R_OKMTO_FN = "r_okmto"
  val BANK_NAME_FN = "bank_name"
  val BANK_BIK_FN = "bank_bik"
  val BANK_BKK_FN = "bank_bkk"
  val ACCOUNT_NUMBER_FN = "account_number"
  val COMMENT_PREFIX_FN = "comment_prefix"
  val COMMENT_SUFFIX_FN = "comment_suffix"

  override val rowParser: RowParser[MBillPayReqsRu] = {
    get[Option[Int]](ID_FN) ~ get[Int](CONTRACT_ID_FN) ~
      get[String](R_NAME_FN) ~ get[Long](R_INN_FN) ~ get[Long](R_KPP_FN) ~ get[Option[Long]](R_OKATO_FN) ~ get[Option[Long]](R_OKMTO_FN) ~
      get[String](BANK_NAME_FN) ~ get[Long](BANK_BIK_FN) ~ get[String](BANK_BKK_FN) ~ get[String](ACCOUNT_NUMBER_FN) ~
      get[Option[String]](COMMENT_PREFIX_FN) ~ get[Option[String]](COMMENT_SUFFIX_FN) map {
      case id ~ contractId ~ rName ~ rInn ~ rKpp ~ rOkato ~ rOkmto ~ bankName ~ bankBik ~ bankBkk ~ accNum ~ commPrefix ~ commSuffix =>
        MBillPayReqsRu(
          id = id, contractId = contractId,
          rName = rName,  rInn = rInn,  rKpp = rKpp,  rOkato = rOkato,  rOkmto = rOkmto,
          bankName = bankName,  bankBik = bankBik,  bankBkk = bankBkk, accountNumber = accNum,
          commentPrefix = commPrefix, commentSuffix = commSuffix
        )
    }
  }

  /** Десериализация экземпляра модели из json. Используется вместе с [[MInviteRequest]]. */
  val fromJson: PartialFunction[Any, MBillPayReqsRu] = {
    case jmap: ju.Map[_,_] =>
      import io.suggest.model.EsModel.{stringParser, intParser, longParser}
      MBillPayReqsRu(
        contractId = intParser(jmap get CONTRACT_ID_FN),
        rName = stringParser(jmap get R_NAME_FN),
        rInn = longParser(jmap get R_INN_FN),
        rKpp = longParser(jmap get R_KPP_FN),
        rOkato = Option(jmap get R_OKATO_FN) map longParser,
        rOkmto = Option(jmap get R_OKMTO_FN) map longParser,
        bankName = stringParser(jmap get BANK_NAME_FN),
        bankBik = longParser(jmap get BANK_BIK_FN),
        bankBkk = stringParser(jmap get BANK_BKK_FN),
        accountNumber = stringParser(jmap get ACCOUNT_NUMBER_FN),
        commentPrefix = Option(jmap get COMMENT_PREFIX_FN) map stringParser,
        commentSuffix = Option(jmap get COMMENT_SUFFIX_FN) map stringParser,
        id = Option(jmap get ID_FN) map intParser
      )
  }
}

import MBillPayReqsRu._

case class MBillPayReqsRu(
  contractId  : Int,
  rName       : String,
  rInn        : Long,
  rKpp        : Long,
  rOkato      : Option[Long],
  rOkmto      : Option[Long],
  bankName    : String,
  bankBik     : Long,
  bankBkk     : String,
  accountNumber: String,
  commentPrefix: Option[String] = None,
  commentSuffix: Option[String] = None,
  id          : Option[Int] = None
) extends SqlModelSave[MBillPayReqsRu] with MBillContractSel with SqlModelDelete with ToPlayJsonObj {

  override def hasId = id.isDefined
  override def companion = MBillPayReqsRu

  override def saveInsert(implicit c: Connection): MBillPayReqsRu = {
    SQL(s"INSERT INTO $TABLE_NAME ($CONTRACT_ID_FN, $R_NAME_FN, $R_INN_FN, $R_KPP_FN, $R_OKATO_FN, $R_OKMTO_FN, $BANK_NAME_FN, $BANK_BIK_FN, $BANK_BKK_FN, $ACCOUNT_NUMBER_FN, $COMMENT_PREFIX_FN, $COMMENT_SUFFIX_FN)" +
      " VALUES({contractId}, {rName}, {rInn}, {rKpp}, {rOkato}, {rOkmto}, {bankName}, {bankBik}, {bankBkk}, {accNum}, {commPref}, {commSuf})")
    .on('contractId -> contractId, 'rName -> rName, 'rInn -> rInn, 'rKpp -> rKpp, 'rOkato -> rOkato, 'rOkmto -> rOkmto,
        'bankName -> bankName, 'bankBik -> bankBik, 'bankBkk -> bankBkk, 'accNum -> accountNumber,
        'commPref -> commentPrefix, 'commSuf -> commentSuffix)
    .executeInsert(rowParser single)
  }

  override def saveUpdate(implicit c: Connection): Int = {
    SQL(s"UPDATE $TABLE_NAME SET $R_NAME_FN = {rName}, $R_INN_FN = {rInn}, $R_KPP_FN = {rKpp}, $R_OKATO_FN = {rOkato}, $R_OKATO_FN = {rOkmto}," +
      s"$BANK_NAME_FN = {bankName}, $BANK_BIK_FN = {bankBik}, $BANK_BKK_FN = {bankBkk}, $ACCOUNT_NUMBER_FN = {accNum}," +
      s"$COMMENT_PREFIX_FN = {commPref}, $COMMENT_SUFFIX_FN = {commSuf} WHERE $ID_FN = {id}")
    .on('rName -> rName, 'rInn -> rInn, 'rKpp -> rKpp, 'rOkato -> rOkato, 'rOkmto -> rOkmto,
        'bankName -> bankName, 'bankBik -> bankBik, 'bankBkk -> bankBkk, 'accNum -> accountNumber,
        'commPref -> commentPrefix, 'commSuf -> commentSuffix, 'id -> id.get)
    .executeUpdate()
  }

  /** Сериалиазация экземпляра модели в промежуточное представление play.Json. Используется для [[MInviteRequest]]. */
  override def toPlayJsonAcc: FieldsJsonAcc = {
    var acc: FieldsJsonAcc = List(
      CONTRACT_ID_FN  -> JsNumber(contractId),
      R_NAME_FN       -> JsString(rName),
      R_INN_FN        -> JsNumber(rInn),
      R_KPP_FN        -> JsNumber(rKpp),
      BANK_NAME_FN    -> JsString(bankName),
      BANK_BIK_FN     -> JsNumber(bankBik),
      BANK_BKK_FN     -> JsString(bankBkk),
      ACCOUNT_NUMBER_FN -> JsString(accountNumber)
    )
    if (rOkato.isDefined)
      acc ::= R_OKATO_FN -> JsNumber(rOkato.get)
    if (rOkmto.isDefined)
      acc ::= R_OKMTO_FN -> JsNumber(rOkmto.get)
    if (commentPrefix.isDefined)
      acc ::= COMMENT_PREFIX_FN -> JsString(commentPrefix.get)
    if (commentSuffix.isDefined)
      acc ::= COMMENT_SUFFIX_FN -> JsString(commentSuffix.get)
    acc
  }

  override def toPlayJsonWithId: JsObject = {
    var acc = toPlayJsonAcc
    if (id.isDefined)
      acc ::= ID_FN -> JsNumber(id.get)
    JsObject(acc)
  }
}

