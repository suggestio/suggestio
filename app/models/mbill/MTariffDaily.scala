package models.mbill

import java.sql.Connection
import java.{util => ju}

import anorm._
import io.suggest.model.es.EsModelUtil.FieldsJsonAcc
import io.suggest.model.es.{EsModelUtil, ToPlayJsonObj}
import models._
import play.api.Play.{configuration, current}
import play.api.libs.json.{JsNumber, JsObject, JsString}
import util.sqlm.SqlModelSave

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 27.05.14 11:52
 * Description: Модель работы с тарификациями при посуточной оплате рекламных модулей.
 * mmp = minimal module price.
 */
object MTariffDaily extends FindByContract with FromJson {
  import SqlParser._

  override type T = MTariffDaily

  val TABLE_NAME = "bill_mmp_daily"

  val ID_FN             = "id"
  val CONTRACT_ID_FN    = "contract_id"
  val MMP_WEEKDAY_FN    = "mmp_weekday"
  val MMP_WEEKEND_FN    = "mmp_weekend"
  val MMP_PRIMETIME_FN  = "mmp_primetime"
  val CURRENCY_CODE_FN  = "currency_code"
  val ON_START_PAGE_FN  = "on_start_page"
  val WEEKEND_CAL_ID_FN = "weekend_cal_id"
  val PRIME_CAL_ID_FN   = "prime_cal_id"
  val ON_RCVR_CAT_FN    = "on_rcvr_cat"


  val rowParser = {
    get[Option[Int]](ID_FN) ~ get[Int](CONTRACT_ID_FN) ~ get[Float](MMP_WEEKDAY_FN) ~
      get[Float](MMP_WEEKEND_FN) ~ get[Float](MMP_PRIMETIME_FN) ~ get[String](CURRENCY_CODE_FN) ~
      get[Float](ON_START_PAGE_FN) ~ get[String](WEEKEND_CAL_ID_FN) ~ get[String](PRIME_CAL_ID_FN) ~
      get[Float](ON_RCVR_CAT_FN) map {
      case id ~ contractId ~ mmpWeekday ~ mmpWeekend ~ mmpPrimetime ~ currencyCode ~ onStartPage ~
        weekendCalId ~ primeCalId ~ onRcvrCat =>
        MTariffDaily(
          id = id, contractId = contractId, currencyCode = currencyCode,
          mmpWeekday = mmpWeekday, mmpWeekend = mmpWeekend, mmpPrimetime = mmpPrimetime,
          onRcvrCat = onRcvrCat, onStartPage = onStartPage, weekendCalId = weekendCalId, primeCalId = primeCalId
        )
    }
  }

  /** Найти все adnId через таблицу контрактов.
    * @return Список adnId без повторений в неопределённом порядке.
    */
  def findAllAdnIds(implicit c: Connection): List[String] = {
    SQL(s"SELECT DISTINCT mbc.${MContract.ADN_ID_FN} FROM ${MContract.TABLE_NAME} mbc WHERE mbc.${MContract.ID_FN} IN (SELECT DISTINCT $CONTRACT_ID_FN FROM $TABLE_NAME)")
      .as(MContract.ADN_ID_PARSER *)
  }

  /** Найти все ряды, в которых встречается указанный календарь. */
  def findForCalId(calId: String)(implicit c: Connection): List[MTariffDaily] = {
    SQL(s"SELECT * FROM $TABLE_NAME WHERE $WEEKEND_CAL_ID_FN = {calId} OR $PRIME_CAL_ID_FN = {calId}")
      .on('calId -> calId)
      .as(rowParser *)
  }

  /**
   * Обновить (reset) всю таблицу по шаблону.
   * @param template Шаблон.
   * @return Кол-во обновлённых рядов.
   */
  def updateAll(template: MTariffDaily)(implicit c: Connection): Int = {
    SQL(updateSqlPreamble)
      .on(template.dataSqlArgs : _*)
      .executeUpdate()
  }

  /** Десериализация того, что хранилось в виде JSON, например внутри [[MInviteRequest]]. */
  val fromJson: PartialFunction[Any, MTariffDaily] = {
    case jmap: ju.Map[_,_] =>
      import EsModelUtil.{floatParser, intParser, stringParser}
      MTariffDaily(
        contractId    = intParser(jmap get CONTRACT_ID_FN),
        mmpWeekday    = floatParser(jmap get MMP_WEEKDAY_FN),
        mmpWeekend    = floatParser(jmap get MMP_WEEKEND_FN),
        mmpPrimetime  = floatParser(jmap get MMP_PRIMETIME_FN),
        onRcvrCat     = floatParser(jmap get ON_RCVR_CAT_FN),
        onStartPage   = floatParser(jmap get ON_START_PAGE_FN),
        weekendCalId  = stringParser(jmap get WEEKEND_CAL_ID_FN),
        primeCalId    = stringParser(jmap get PRIME_CAL_ID_FN),
        currencyCode  = stringParser(jmap get CURRENCY_CODE_FN),
        id            = Option(jmap get ID_FN) map intParser
      )
  }

  object Dflts {
    private def c(key: String): String = {
      "sys.mmp.daily." + key + ".dflt"
    }
    private def s(key: String): Option[String] = {
      configuration.getString(c(key))
    }
    private def d(key: String): Option[Double] = {
      configuration.getDouble(c(key))
    }
    // Дефолтовые значения для формы создания нового mmp-тарификатора.
    val CURRENCY_CODE   = s("currency.code")
      .fold(CurrencyCodeOpt.CURRENCY_CODE_DFLT)(_.toUpperCase)
    val WEEKDAY         = d("weekday")
      .fold(1.0F)(_.toFloat)
    val WEEKEND         = d("weekend")
      .fold(1.5F)(_.toFloat)
    val PRIME           = d("prime")
      .fold(2.0F)(_.toFloat)
    val ON_START_PAGE   = d("on.startPage")
      .fold(4.0F)(_.toFloat)
    val ON_RCVR_CAT     = d("on.rcvrCat")
      .fold(2.0F)(_.toFloat)
    val CAL_ID_WEEKEND  = s("calId.weekend")
      .getOrElse("")
    val CAL_ID_PRIME    = s("calId.prime")
      .getOrElse(CAL_ID_WEEKEND)
  }

  private def updateSqlPreamble: String = {
    s"UPDATE $TABLE_NAME SET $CURRENCY_CODE_FN = {currencyCode}, $MMP_WEEKDAY_FN = {mmpWeekday}, " +
      s"$MMP_WEEKEND_FN = {mmpWeekend}, $MMP_PRIMETIME_FN = {mmpPrimetime}, $ON_START_PAGE_FN = {onStartPage}, " +
      s"$ON_RCVR_CAT_FN = {onRcvrCat}, $WEEKEND_CAL_ID_FN = {weekendCalId}, $PRIME_CAL_ID_FN = {primeCalId} "
  }

}


import models.mbill.MTariffDaily._


final case class MTariffDaily(
  contractId    : Int,
  mmpWeekday    : Float   = MTariffDaily.Dflts.WEEKDAY,
  mmpWeekend    : Float   = MTariffDaily.Dflts.WEEKEND,
  mmpPrimetime  : Float   = MTariffDaily.Dflts.PRIME,
  onRcvrCat     : Float   = MTariffDaily.Dflts.ON_RCVR_CAT,
  onStartPage   : Float   = MTariffDaily.Dflts.ON_START_PAGE,
  weekendCalId  : String  = MTariffDaily.Dflts.CAL_ID_WEEKEND,
  primeCalId    : String  = MTariffDaily.Dflts.CAL_ID_PRIME,
  currencyCode  : String  = CurrencyCodeOpt.CURRENCY_CODE_DFLT,
  id            : Option[Int] = None
) extends SqlModelSave with CurrencyCode with MContractSel with SqlModelDelete with ToPlayJsonObj {

  override def hasId = id.isDefined
  override def companion = MTariffDaily
  override type T = MTariffDaily

  override def saveInsert(implicit c: Connection): T = {
    val args = ('contractId -> contractId : NamedParameter) :: dataSqlArgs
    SQL(s"INSERT INTO $TABLE_NAME ($CONTRACT_ID_FN, $CURRENCY_CODE_FN, $MMP_WEEKDAY_FN, $MMP_WEEKEND_FN, $MMP_PRIMETIME_FN, $ON_START_PAGE_FN, $WEEKEND_CAL_ID_FN, $PRIME_CAL_ID_FN, $ON_RCVR_CAT_FN) " +
      "VALUES ({contractId}, {currencyCode}, {mmpWeekday}, {mmpWeekend}, {mmpPrimetime}, {onStartPage}, {weekendCalId}, {primeCalId}, {onRcvrCat})")
      .on(args: _*)
      .executeInsert(rowParser single)
  }

  /** Аргументы для update */
  def dataSqlArgs = {
    List[NamedParameter](
      'currencyCode -> currencyCode,  'mmpWeekday   -> mmpWeekday,    'mmpWeekend   -> mmpWeekend,
      'onRcvrCat    -> onRcvrCat,     'mmpPrimetime -> mmpPrimetime,  'onStartPage  -> onStartPage,
      'weekendCalId -> weekendCalId,  'primeCalId   -> primeCalId
    )
  }

  override def saveUpdate(implicit c: Connection): Int = {
    val args = ('id -> id.get : NamedParameter) :: dataSqlArgs
    SQL(s"$updateSqlPreamble WHERE $ID_FN = {id}")
      .on(args : _*)
      .executeUpdate()
  }

  /** Сериализация модели в JSON. Например, для нужд [[MInviteRequest]]. */
  // TODO MarketJoinRequest не прижился, а этот, если нужен, код следует переписать через play.json, а не так всырую.
  override def toPlayJsonAcc: FieldsJsonAcc = {
    List(
      CONTRACT_ID_FN    -> JsNumber(contractId),
      MMP_WEEKDAY_FN    -> JsNumber(mmpWeekday),
      MMP_WEEKEND_FN    -> JsNumber(mmpWeekend),
      MMP_PRIMETIME_FN  -> JsNumber(mmpPrimetime),
      ON_RCVR_CAT_FN    -> JsNumber(onRcvrCat),
      ON_START_PAGE_FN  -> JsNumber(onStartPage),
      WEEKEND_CAL_ID_FN -> JsString(weekendCalId),
      PRIME_CAL_ID_FN   -> JsString(primeCalId),
      CURRENCY_CODE_FN  -> JsString(currencyCode)
    )
  }

  override def toPlayJsonWithId: JsObject = {
    var acc = toPlayJsonAcc
    if (id.isDefined)
      acc ::= ID_FN -> JsNumber(id.get)
    JsObject(acc)
  }
}
