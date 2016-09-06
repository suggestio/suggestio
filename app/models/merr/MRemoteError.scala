package models.merr

import io.suggest.model.es._
import EsModelUtil.FieldsJsonAcc
import com.google.inject.{Inject, Singleton}
import io.suggest.util.SioEsUtil._
import models._
import models.mproj.ICommonDi
import org.joda.time.DateTime
import util.PlayMacroLogsImpl
import play.api.libs.json._
import play.api.libs.functional.syntax._

import scala.collection.Map
import scala.concurrent.ExecutionContext

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 14.10.14 18:55
 * Description: Модель для хранения ошибок на клиентах. Информация имеет TTL, настраив
 */
@Singleton
class MRemoteErrors @Inject() (
  override val mCommonDi: ICommonDi
)
  extends EsModelStatic
    with PlayMacroLogsImpl
    with EsmV2Deserializer
    with EsModelPlayJsonStaticT
{

  override type T = MRemoteError

  override def ES_INDEX_NAME = EsModelUtil.GARBAGE_INDEX
  override val ES_TYPE_NAME = "rerr"

  // Имена полей. По возможности должны совпадать с названиями в MAdStat.
  def ERROR_TYPE_FN             = "errType"

  def MESSAGE_FN                = "msg"
  def URL_FN                    = "url"
  def TIMESTAMP_FN              = "timestamp"
  def CLIENT_ADDR_FN            = "clientAddr"
  def UA_FN                     = "ua"

  def CLIENT_IP_GEO_FN          = "clIpGeo"
  def CLIENT_TOWN_FN            = "clIpTown"
  def COUNTRY_FN                = "country"
  def IS_LOCAL_CLIENT_FN        = "isLocalCl"
  def STATE_FN                  = "state"

  /** Время хранения данных в модели. */
  def TTL_DAYS = 30

  override def generateMappingStaticFields: List[Field] = {
    List(
      FieldAll(enabled = true),
      FieldSource(enabled = true),
      FieldTtl(enabled = true, default = s"${TTL_DAYS}d")
    )
  }

  /**
   * Индексируем всё, т.к. модель ориентирована на быстрое исправление ошибок.
   * Мусор из индекса будет вычищен по TTL.
   *
   * @return
   */
  override def generateMappingProps: List[DocField] = {
    List(
      FieldString(ERROR_TYPE_FN, index = FieldIndexingVariants.not_analyzed, include_in_all = true),
      FieldString(MESSAGE_FN, index = FieldIndexingVariants.analyzed, include_in_all = true),
      FieldString(URL_FN, index = FieldIndexingVariants.analyzed, include_in_all = true),
      FieldDate(TIMESTAMP_FN, index = null, include_in_all = true),
      FieldString(CLIENT_ADDR_FN, index = FieldIndexingVariants.analyzed, include_in_all = true),
      FieldString(UA_FN, index = FieldIndexingVariants.analyzed, include_in_all = true),
      FieldGeoPoint(CLIENT_IP_GEO_FN, geohash = true, geohashPrecision = "5", geohashPrefix = true,
        fieldData = GeoPointFieldData(format = GeoPointFieldDataFormats.compressed, precision = "5km")),
      FieldString(CLIENT_TOWN_FN, index = FieldIndexingVariants.analyzed, include_in_all = true),
      FieldString(COUNTRY_FN, index = FieldIndexingVariants.not_analyzed, include_in_all = true),
      FieldBoolean(IS_LOCAL_CLIENT_FN, index = FieldIndexingVariants.not_analyzed, include_in_all = false),
      FieldString(STATE_FN, index = FieldIndexingVariants.analyzed, include_in_all = true)
    )
  }

  /**
   * Десериализация одного элементам модели.
   *
   * @param id id документа.
   * @param m Карта, распарсенное json-тело документа.
   * @return Экземпляр модели.
   */
  @deprecated("Delete it", "2015.sep.7")
  override def deserializeOne(id: Option[String], m: Map[String, AnyRef], version: Option[Long]): T = {
    import EsModelUtil.{booleanParser, dateTimeParser, stringParser}
    def parseStr(fn: String) = m.get(fn).fold("")(stringParser)
    MRemoteError(
      errorType   = m.get(ERROR_TYPE_FN)
        .map(stringParser)
        .flatMap(MRemoteErrorTypes.maybeWithName)
        .getOrElse(MRemoteErrorTypes.values.head),
      msg         = parseStr(MESSAGE_FN),
      clientAddr  = parseStr(CLIENT_ADDR_FN),
      ua          = m.get(UA_FN)
        .map(stringParser),
      url         = m.get(URL_FN)
        .map(stringParser),
      timestamp   = m.get(TIMESTAMP_FN)
        .fold(DateTime.now)(dateTimeParser),
      clIpGeo     = m.get(CLIENT_IP_GEO_FN)
        .flatMap(GeoPoint.deserializeOpt),
      clTown      = m.get(CLIENT_TOWN_FN)
        .map(stringParser),
      country     = m.get(COUNTRY_FN)
        .map(stringParser),
      isLocalCl   = m.get(IS_LOCAL_CLIENT_FN)
        .map(booleanParser),
      state       = m.get(STATE_FN)
        .map(stringParser),
      id          = id
    )
  }

  // Инстанс почти-готового к работе JSON-десериализатора. Пока не нужен, поэтому тут lazy val вместо val.
  private lazy val _reads0 = {
    (__ \ ERROR_TYPE_FN).read[MRemoteErrorType] and
    (__ \ MESSAGE_FN).read[String] and
    (__ \ CLIENT_ADDR_FN).read[String] and
    (__ \ UA_FN).readNullable[String] and
    (__ \ TIMESTAMP_FN).readNullable[DateTime]
      .map { _ getOrElse DateTime.now } and
    (__ \ URL_FN).readNullable[String] and
    (__ \ CLIENT_IP_GEO_FN).readNullable[GeoPoint] and
    (__ \ CLIENT_TOWN_FN).readNullable[String] and
    (__ \ COUNTRY_FN).readNullable[String] and
    (__ \ IS_LOCAL_CLIENT_FN).readNullable[Boolean] and
    (__ \ STATE_FN).readNullable[String]
  }

  override protected def esDocReads(meta: IEsDocMeta): Reads[MRemoteError] = {
    _reads0 {
      (errTyp, msg, clAddr, ua, tstamp, url, clIpGeo, clTown, country, isLocalCl, state) =>
        MRemoteError(errTyp, msg, clAddr, ua, tstamp, url, clIpGeo, clTown, country = country, isLocalCl, state, meta.id)
    }
  }


  override def writeJsonFields(m: T, acc0: FieldsJsonAcc): FieldsJsonAcc = {
    import m._
    var acc: FieldsJsonAcc =
      ERROR_TYPE_FN   -> JsString(errorType.toString) ::
      MESSAGE_FN          -> JsString(msg) ::
      TIMESTAMP_FN    -> EsModelUtil.date2JsStr(timestamp) ::
      CLIENT_ADDR_FN  -> JsString(clientAddr) ::
      acc0
    if (ua.isDefined)
      acc ::= UA_FN -> JsString(ua.get)
    if (url.isDefined)
      acc ::= URL_FN -> JsString(url.get)
    if (clIpGeo.isDefined)
      acc ::= CLIENT_IP_GEO_FN -> clIpGeo.get.toPlayGeoJson
    if (clTown.isDefined)
      acc ::= CLIENT_TOWN_FN -> JsString(clTown.get)
    if (country.isDefined)
      acc ::= COUNTRY_FN -> JsString(country.get)
    if (isLocalCl.isDefined)
      acc ::= IS_LOCAL_CLIENT_FN -> JsBoolean(isLocalCl.get)
    if (state.isDefined)
      acc ::= STATE_FN -> JsString(state.get)
    acc
  }


}

/** Интерфейс для поля с DI-инстансом [[MRemoteErrors]]. */
trait IMRemoteErrors {
  def mRemoteErrors: MRemoteErrors
}



/**
 * Экземпляр модели.
 *
 * @param msg Сообщение об ошибке.
 * @param url Ссылка, относящаяся к ошибке.
 * @param clientAddr ip-адрес клиента.
 * @param ua Юзер-агент клиента.
 * @param timestamp Дата-время получения сообщения.
 * @param clIpGeo Примерные координаты клиента по мнению geoip.
 * @param clTown Название города, в котором находится клиента, по мнению geoip.
 * @param country Страна метоположения клиента по мнению geoip.
 * @param id id документа.
 */
case class MRemoteError(
  errorType   : MRemoteErrorType,
  msg         : String,
  clientAddr  : String,
  ua          : Option[String]    = None,
  timestamp   : DateTime          = DateTime.now,
  url         : Option[String]    = None,
  clIpGeo     : Option[GeoPoint]  = None,
  clTown      : Option[String]    = None,
  country     : Option[String]    = None,
  isLocalCl   : Option[Boolean]   = None,
  state       : Option[String]    = None,
  id          : Option[String]    = None
)
  extends EsModelT
{

  /** Версия тут не нужна, т.к. модель write-only, и читается через kibana. */
  override def versionOpt = None

}



// JMX-утиль

trait MRemoteErrorsJmxMBean extends EsModelJMXMBeanI
final class MRemoteErrorsJmx @Inject() (
  override val companion  : MRemoteErrors,
  override val ec         : ExecutionContext
)
  extends EsModelJMXBaseImpl
    with MRemoteErrorsJmxMBean
{
  override type X = MRemoteError
}

