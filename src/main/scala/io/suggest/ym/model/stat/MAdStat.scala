package io.suggest.ym.model.stat

import java.text.SimpleDateFormat
import java.{lang => jl}

import com.google.inject.{Inject, Singleton}
import com.sun.org.glassfish.gmbal.{Description, Impact, ManagedOperation}
import io.suggest.model.es.EsModelUtil._
import io.suggest.model.es._
import io.suggest.model.geo.GeoPoint
import io.suggest.util.MacroLogsImpl
import io.suggest.util.SioEsUtil._
import org.elasticsearch.action.index.IndexRequestBuilder
import org.elasticsearch.index.query.QueryBuilders
import org.elasticsearch.search.sort.SortOrder
import org.joda.time.{DateTime, DateTimeZone}
import play.api.Configuration
import play.api.libs.json._

import scala.concurrent.duration.FiniteDuration
import scala.concurrent.{ExecutionContext, Future}

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 31.03.14 15:57
  * Description: Для накопления статистики по рекламным карточкам используется эта модель.
  *
  * 2016.aug.26: Здесь до util:a65801b6da4f включитель жил код для построения какой-то гистограммы по датам.
  * При переезде на es 2.0 оказалось, что этот код не компилится и не используется. Он был удалён.
  */
@Singleton
class MAdStats @Inject() (
  configuration           : Configuration,
  override val mCommonDi  : IEsModelDiVal
)
  extends EsModelStatic
    with MacroLogsImpl
    with EsModelPlayJsonStaticT
{
  import mCommonDi._

  override type T = MAdStat

  /** Используем изолированный индекс для статистики из-за крайней суровости статистической информации. */
  override def ES_INDEX_NAME = EsModelUtil.GARBAGE_INDEX
  override val ES_TYPE_NAME = "adStat"

  // Поля модели.
  val CLIENT_ADDR_ESFN          = "clientAddr"
  val ACTION_ESFN               = "action"
  val UA_ESFN                   = "ua"
  val AD_ID_ESFN                = "adId"
  val ADS_RENDERED_ESFN         = "adsRendered"
  val ON_NODE_ID_ESFN           = "adOwnerId"
  val TIMESTAMP_ESFN            = "timestamp"

  val CLIENT_IP_GEO_EFSN        = "clIpGeo"
  val CLIENT_TOWN_ESFN          = "clIpTown"
  val CLIENT_GEO_LOC_ESFN       = "clLoc"
  val NODE_NAME_ESFN            = "nodeName"
  val COUNTRY_ESFN              = "country"
  val IS_LOCAL_CLIENT_ESFN      = "isLocalCl"
  val CLIENT_LOC_ACCUR_ESFN     = "clLocAccur"

  val CL_OS_FAMILY_ESFN         = "osFamily"
  val CL_AGENT_ESFN             = "browser"
  val CL_DEVICE_ESFN            = "device"
  val CLICKED_AD_ID_ESFN        = "clickedAdId"
  val GENERATION_ESFN           = "gen"
  val CL_OS_VERSION_ESFN        = "osVsn"
  val CL_UID_ESFN               = "clUID"

  val SCREEN_ORIENTATION_ESFN   = "scrOrient"
  val SCREEN_RES_CHOOSEN_ESFN   = "scrResChoosen"
  val PX_RATIO_CHOOSEN_ESFN     = "pxRatioChoosen"
  val VIEWPORT_DECLARED_ESFN    = "viewportDecl"

  val REQUEST_URI_ESFN          = "uri"


  /** Через сколько времени удалять записи статистики. */
  val TTL_DAYS_DFLT = configuration.getInt("ad.stat.ttl.period.days").getOrElse(100)

  type AdFreqs_t = Map[String, Map[String, Long]]

  def adOwnerQuery(adOwnerId: String) = QueryBuilders.termQuery(ON_NODE_ID_ESFN, adOwnerId)

  def beforeDtQuery(dt: DateTime) = {
    QueryBuilders.rangeQuery(TIMESTAMP_ESFN)
      .from(new DateTime(1970, 1, 1, 0, 0))
      .to(dt)
  }

  /** Подсчёт кол-ва вхождений до указанной даты. */
  def countBefore(dt: DateTime): Future[Long] = {
    prepareCount()
      .setQuery( beforeDtQuery(dt) )
      .execute()
      .map { _.getCount }
  }

  /** Найти все вхождения до указанной даты. */
  def findBefore(dt: DateTime, maxResults: Int = MAX_RESULTS_DFLT): Future[Seq[T]] = {
    prepareSearch()
      .setQuery( beforeDtQuery(dt) )
      .setSize(maxResults)
      .addSort(TIMESTAMP_ESFN, SortOrder.ASC)
      .execute()
      .map { searchResp2list }
  }

  /** Удалить все данные до указанной даты. */
  def deleteBefore(dt: DateTime): Future[Int] = {
    val scroller = startScroll(
      queryOpt          = Some(beforeDtQuery(dt)),
      resultsPerScroll  = BULK_DELETE_QUEUE_LEN / 2
    )
    deleteByQuery(scroller)
  }


  /**
    * Десериализация одного элементам модели.
    *
    * @param id id документа.
    * @param m Карта, распарсенное json-тело документа.
    * @return Экземпляр модели.
    */
  override def deserializeOne(id: Option[String], m: collection.Map[String, AnyRef], version: Option[Long]): T = {
    new MAdStat(
      clientAddr  = m.get(CLIENT_ADDR_ESFN).fold("127.0.0.1")(stringParser),
      action      = m.get(ACTION_ESFN).fold("v")(stringParser),
      adIds       = {
        m.get(AD_ID_ESFN).fold (Seq.empty[String]) {
          case adIdsRaw: jl.Iterable[_] => strListParser(adIdsRaw)
          case other => Seq(stringParser(other))
        }
      },
      adsRendered     = m.get(ADS_RENDERED_ESFN).fold(0)(intParser),
      isLocalCl       = m.get(IS_LOCAL_CLIENT_ESFN).fold(false)(booleanParser),
      onNodeIdOpt     = m.get(ON_NODE_ID_ESFN).map(stringParser),
      ua              = m.get(UA_ESFN).map(stringParser),
      nodeName        = m.get(NODE_NAME_ESFN).map(stringParser),
      personId        = m.get(PERSON_ID_ESFN).map(stringParser),
      timestamp       = m.get(TIMESTAMP_ESFN).fold(new DateTime(1970,0,0))(dateTimeParser),
      clIpGeo         = m.get(CLIENT_IP_GEO_EFSN).flatMap(GeoPoint.deserializeOpt),
      clTown          = m.get(CLIENT_TOWN_ESFN).map(stringParser),
      clGeoLoc        = m.get(CLIENT_GEO_LOC_ESFN).flatMap(GeoPoint.deserializeOpt),
      clCountry       = m.get(COUNTRY_ESFN).map(stringParser),
      clLocAccur      = m.get(CLIENT_LOC_ACCUR_ESFN).map(intParser),
      clOSFamily      = m.get(CL_OS_FAMILY_ESFN).map(stringParser),
      clAgent         = m.get(CL_AGENT_ESFN).map(stringParser),
      clDevice        = m.get(CL_DEVICE_ESFN).map(stringParser),
      clickedAdIds    = m.get(CLICKED_AD_ID_ESFN).fold(Seq.empty[String])(strListParser),
      generation      = m.get(GENERATION_ESFN).map(longParser),
      clOsVsn         = m.get(CL_OS_VERSION_ESFN).map(stringParser),
      clUid           = m.get(CL_UID_ESFN).map(stringParser),
      scrOrient       = m.get(SCREEN_ORIENTATION_ESFN).map(stringParser),
      scrResChoosen   = m.get(SCREEN_RES_CHOOSEN_ESFN).map(stringParser),
      pxRatioChoosen  = m.get(PX_RATIO_CHOOSEN_ESFN).map(intParser).map(v => v.toFloat / 10F),
      viewportDecl    = m.get(VIEWPORT_DECLARED_ESFN).map(stringParser),
      reqUri          = m.get(REQUEST_URI_ESFN).map(stringParser),
      id              = id
    )
  }


  /** Статические поля для маппиннга. */
  override def generateMappingStaticFields: List[Field] = List(
    // es-2.0: field _id is not changeable.
    //FieldId(index = FieldIndexingVariants.no, store = false),
    FieldSource(enabled = true),
    FieldAll(enabled = true),
    FieldTtl(enabled = true, default = s"${TTL_DAYS_DFLT}d")
  )

  /** Маппинги для типа этой модели. */
  override def generateMappingProps: List[DocField] = {
    import DocFieldTypes._
    import FieldIndexingVariants._
    import GeoPointFieldDataFormats._
    List(
      FieldString(CLIENT_ADDR_ESFN, index = analyzed, include_in_all = true),
      FieldString(ACTION_ESFN, index = not_analyzed, include_in_all = false),
      FieldString(UA_ESFN, index = analyzed, include_in_all = true),
      FieldString(AD_ID_ESFN, index = not_analyzed, include_in_all = false),
      FieldNumber(ADS_RENDERED_ESFN, index = analyzed, include_in_all = true, fieldType = integer),
      FieldString(ON_NODE_ID_ESFN, index = not_analyzed, include_in_all = false),
      FieldDate(TIMESTAMP_ESFN, index = null, include_in_all = false),
      FieldString(PERSON_ID_ESFN, index = not_analyzed, include_in_all = false),
      FieldGeoPoint(CLIENT_IP_GEO_EFSN, geohash = true, geohashPrecision = "5", geohashPrefix = true,
        fieldData = GeoPointFieldData(format = compressed, precision = "5km")),
      FieldString(CLIENT_TOWN_ESFN, index = analyzed, include_in_all = true),
      FieldGeoPoint(CLIENT_GEO_LOC_ESFN, geohash = true, geohashPrecision = "6", geohashPrefix = true,
        fieldData = GeoPointFieldData(format = compressed, precision = "10m")),
      FieldString(NODE_NAME_ESFN, index = not_analyzed, include_in_all = true),
      FieldString(COUNTRY_ESFN, index = not_analyzed, include_in_all = true),
      FieldBoolean(IS_LOCAL_CLIENT_ESFN, index = not_analyzed, include_in_all = false),
      FieldNumber(CLIENT_LOC_ACCUR_ESFN, index = not_analyzed, include_in_all = true, fieldType = DocFieldTypes.integer),
      FieldString(CL_OS_FAMILY_ESFN, index = not_analyzed, include_in_all = true),
      FieldString(CL_AGENT_ESFN, index = not_analyzed, include_in_all = true),
      FieldString(CL_DEVICE_ESFN, index = not_analyzed, include_in_all = true),
      FieldString(CLICKED_AD_ID_ESFN, index = not_analyzed, include_in_all = false),
      FieldNumber(GENERATION_ESFN, fieldType = long, index = analyzed, include_in_all = false),
      FieldString(CL_OS_VERSION_ESFN, index = not_analyzed, include_in_all = true),
      FieldString(CL_UID_ESFN, index = not_analyzed, include_in_all = false),
      FieldString(SCREEN_ORIENTATION_ESFN, index = not_analyzed, include_in_all = false),
      FieldString(SCREEN_RES_CHOOSEN_ESFN, index = not_analyzed, include_in_all = true),
      FieldNumber(PX_RATIO_CHOOSEN_ESFN, index = not_analyzed, include_in_all = false, fieldType = DocFieldTypes.integer),
      FieldString(VIEWPORT_DECLARED_ESFN, index = no, include_in_all = false),
      FieldString(REQUEST_URI_ESFN, index = no, include_in_all = false)
    )
  }

  override def prepareIndex(m: T): IndexRequestBuilder = {
    val irb = super.prepareIndex(m)
    if (m.ttl.isDefined)
      irb.setTTL(m.ttl.get.toMillis)
    irb
  }

  override def writeJsonFields(m: T, acc: FieldsJsonAcc): FieldsJsonAcc = {
    import m._
    var acc1: FieldsJsonAcc =
      CLIENT_ADDR_ESFN      -> JsString(clientAddr) ::
      ACTION_ESFN           -> JsString(action.toString) ::
      TIMESTAMP_ESFN        -> date2JsStr(timestamp) ::
      IS_LOCAL_CLIENT_ESFN  -> JsBoolean(isLocalCl) ::
      acc
    if (adIds.nonEmpty)
      acc1 ::= AD_ID_ESFN -> JsArray(adIds map JsString.apply)
    if (adsRendered > 0)
      acc1 ::= ADS_RENDERED_ESFN -> JsNumber(adsRendered)
    if (ua.isDefined)
      acc1 ::= UA_ESFN -> JsString(ua.get)
    if (personId.isDefined)
      acc1 ::= PERSON_ID_ESFN -> JsString(personId.get)
    if (onNodeIdOpt.isDefined)
      acc1 ::= ON_NODE_ID_ESFN -> JsString(onNodeIdOpt.get)
    if (clIpGeo.isDefined)
      acc1 ::= CLIENT_IP_GEO_EFSN -> clIpGeo.get.toPlayGeoJson
    if (clTown.isDefined)
      acc1 ::= CLIENT_TOWN_ESFN -> JsString(clTown.get)
    if (clGeoLoc.isDefined)
      acc1 ::= CLIENT_GEO_LOC_ESFN -> clGeoLoc.get.toPlayGeoJson
    if (nodeName.isDefined)
      acc1 ::= NODE_NAME_ESFN -> JsString(nodeName.get)
    if (clCountry.isDefined)
      acc1 ::= COUNTRY_ESFN -> JsString(clCountry.get)
    if (clOSFamily.isDefined)
      acc1 ::= CL_OS_FAMILY_ESFN -> JsString(clOSFamily.get)
    if (clAgent.isDefined)
      acc1 ::= CL_AGENT_ESFN -> JsString(clAgent.get)
    if (clDevice.isDefined)
      acc1 ::= CL_DEVICE_ESFN -> JsString(clDevice.get)
    if (clickedAdIds.nonEmpty)
      acc1 ::= CLICKED_AD_ID_ESFN -> JsArray(clickedAdIds.map(JsString.apply))
    if (generation.isDefined)
      acc1 ::= GENERATION_ESFN -> JsNumber(generation.get)
    if (clLocAccur.isDefined)
      acc1 ::= CLIENT_LOC_ACCUR_ESFN -> JsNumber(clLocAccur.get)
    if (clOsVsn.isDefined)
      acc1 ::= CL_OS_VERSION_ESFN -> JsString(clOsVsn.get)
    if (clUid.isDefined)
      acc1 ::= CL_UID_ESFN -> JsString(clUid.get)
    if (scrOrient.isDefined)
      acc1 ::= SCREEN_ORIENTATION_ESFN -> JsString(scrOrient.get)
    if (scrResChoosen.isDefined)
      acc1 ::= SCREEN_RES_CHOOSEN_ESFN -> JsString(scrResChoosen.get)
    // Храним и индексируем через 10 x int, чтобы избежать лишней нагрузки на индекс из-за неточности float.
    if (pxRatioChoosen.isDefined)
      acc1 ::= PX_RATIO_CHOOSEN_ESFN -> JsNumber((pxRatioChoosen.get * 10F).toInt)
    if (viewportDecl.isDefined)
      acc1 ::= VIEWPORT_DECLARED_ESFN -> JsString(viewportDecl.get)
    if (reqUri.isDefined)
      acc1 ::= REQUEST_URI_ESFN -> JsString(reqUri.get)
    acc1
  }

}



final class MAdStat(
  val clientAddr          : String,
  val action              : String,
  val adIds               : Seq[String],
  val adsRendered         : Int,
  val isLocalCl           : Boolean,
  val onNodeIdOpt         : Option[String],
  val ua                  : Option[String],
  val nodeName            : Option[String]    = None,
  val personId            : Option[String]    = None,
  val timestamp           : DateTime          = new DateTime,
  val clIpGeo             : Option[GeoPoint]  = None,
  val clTown              : Option[String]    = None,
  val clGeoLoc            : Option[GeoPoint]  = None,
  val clCountry           : Option[String]    = None,
  val clOSFamily          : Option[String]    = None,
  val clAgent             : Option[String]    = None,
  val clDevice            : Option[String]    = None,
  val clLocAccur          : Option[Int]       = None,
  val clickedAdIds        : Seq[String]       = Nil,
  val generation          : Option[Long]      = None,   // запрошенный generation выдачи.
  val clOsVsn             : Option[String]    = None,   // версия OS девайса клиента.
  val clUid               : Option[String]    = None,   // UID клиента, выставленное через transient cookie.
  val scrOrient           : Option[String]    = None,   // Ориентация экрана.
  val scrResChoosen       : Option[String]    = None,   // Выбранное s.io сервером разрешение экрана (viewport'а)
  val pxRatioChoosen      : Option[Float]     = None,   // Выбранный s.io сервером pixel ratio
  val viewportDecl        : Option[String]    = None,   // заявленные размеры экрана (через ?a.screen=[WxH,dpr])
  val reqUri              : Option[String]    = None,   // Исходный путь запроса вместе с qs (для отладки и др.)
  val id                  : Option[String]    = None,
  val ttl                 : Option[FiniteDuration] = None    // Макс.жизни экземпляра в хранилище.
)
  extends EsModelT
{
  override def versionOpt = None
}


// JMX Содержит кое-какие дополнительные функции.

/** JMX MBean интерфейс */
trait MAdStatJmxMBean extends EsModelJMXMBeanI {

  @ManagedOperation(impact = Impact.ACTION)
  @Description("Remove all occurencies BEFORE following timestamp in format: yyyy.MM.dd HH:mm:ss")
  def deleteBefore(dt: String): String

  @ManagedOperation(impact = Impact.INFO)
  @Description("Count all occurencies BEFORE following timestamp in format: yyyy.MM.dd HH:mm:ss")
  def countBefore(dt: String): String

  @ManagedOperation(impact = Impact.INFO)
  @Description("Find all occurenciec BEFORE following timestamp in format: yyyy.MM.dd HH:mm:ss. Second argument sets maxResults returned.")
  def findBefore(dt: String, maxResults: Int): String

}

/** JMX MBean реализация. */
final class MAdStatJmx @Inject() (
  override val companion    : MAdStats,
  override implicit val ec  : ExecutionContext
)
  extends EsModelJMXBase
    with MAdStatJmxMBean
{
  import LOGGER._

  override type X = MAdStat

  protected def dtParse(dtStr: String): DateTime = {
    val sdf = new SimpleDateFormat("yyyy.MM.dd HH:mm:ss")
    val d = sdf.parse(dtStr)
    new DateTime(d)
      .withZone(DateTimeZone.forID("Europe/Moscow"))
  }

  override def deleteBefore(dtStr: String): String = {
    warn(s"deleteBefore($dtStr)")
    // Нужно распарсить дату-время из указанных админом.
    try {
      val dt = dtParse(dtStr)
      val fut = for (countDeleted <- companion.deleteBefore(dt)) yield {
        "OK: count=" + countDeleted
      }
      awaitString(fut)
    } catch {
      case ex: Throwable =>
        error("Unable to parse user-typed date-time: " + dtStr, ex)
        "Failure: user-typed timestamp: " + dtStr + " :: " + ex.getClass.getName + ": " + ex.getMessage
    }
  }

  override def countBefore(dtStr: String): String = {
    trace(s"countBefore($dtStr)")
    try {
      val dt = dtParse(dtStr)
      val fut = for (count <- companion.countBefore(dt)) yield {
        count + " items before " + dt
      }
      awaitString(fut)

    } catch {
      case ex: Throwable =>
        error("Unable to count items for user-typed dt-str: " + dtStr, ex)
        "Failure: user-typed timestamp: " + dtStr + " :: " + ex.getClass.getName + ": " + ex.getMessage
    }
  }

  override def findBefore(dtStr: String, maxResults: Int): String = {
    trace(s"findBefore($dtStr, $maxResults)")
    try {
      val dt = dtParse(dtStr)
      val fut = for (results <- companion.findBefore(dt, maxResults)) yield {
        // Отрендерить результаты в json
        companion.toEsJsonDocs(results)
      }
      awaitString(fut)

    } catch {
      case ex: Throwable =>
        error(s"Failed to findBefore($dtStr, $maxResults)", ex)
        s"FAIL: findBefore($dtStr, $maxResults) ${ex.getClass.getSimpleName} : ${ex.getMessage}"
    }
  }
}

