package io.suggest.ym.model.stat

import java.text.SimpleDateFormat

import com.sun.org.glassfish.gmbal.{Description, Impact, ManagedOperation}
import io.suggest.model._
import io.suggest.model.EsModel._
import io.suggest.ym.model.common.GeoPoint
import org.elasticsearch.action.deletebyquery.DeleteByQueryResponse
import org.elasticsearch.search.sort.SortOrder
import org.joda.time.{DateTimeZone, DateTime}
import org.elasticsearch.common.joda.time.{DateTime => EsDateTime}
import com.fasterxml.jackson.annotation.JsonIgnore
import io.suggest.util.SioEsUtil._
import io.suggest.util.MyConfig.CONFIG
import scala.concurrent.{Future, ExecutionContext}
import org.elasticsearch.client.Client
import org.elasticsearch.index.query.{QueryBuilder, FilterBuilders, QueryBuilders}
import org.elasticsearch.search.aggregations.AggregationBuilders
import scala.collection.JavaConversions._
import org.elasticsearch.search.aggregations.bucket.terms.Terms
import org.elasticsearch.search.aggregations.bucket.histogram.DateHistogram
import io.suggest.event.SioNotifierStaticClientI
import play.api.libs.json.{JsBoolean, JsNumber, JsArray, JsString}
import io.suggest.util.MacroLogsImpl
import java.{lang => jl}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 31.03.14 15:57
 * Description: Для накопления статистики по рекламным карточкам используется эта модель.
 */
object MAdStat extends EsModelMinimalStaticT with MacroLogsImpl {

  override type T = MAdStat

  /** Используем изолированный индекс для статистики из-за крайней суровости статистической информации. */
  override def ES_INDEX_NAME = EsModel.GARBAGE_INDEX
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

  val CL_OS_FAMILY_ESFN         = "osFamily"
  val CL_AGENT_ESFN             = "browser"
  val CL_DEVICE_ESFN            = "device"
  val CLICKED_AD_ID_ESFN        = "clickedAdId"
  val GENERATION_ESFN           = "gen"


  /** Через сколько времени удалять записи статистики. По дефолту - 10 лет. */
  val TTL_DAYS_DFLT = CONFIG.getInt("ad.stat.ttl.period.days") getOrElse 3650

  type AdFreqs_t = Map[String, Map[AdStatAction, Long]]
  type DateHistAds_t = Seq[(EsDateTime, Long)]

  def adOwnerQuery(adOwnerId: String) = QueryBuilders.termQuery(ON_NODE_ID_ESFN, adOwnerId)

  /**
   * Аггрегат, порождающий карту из id реклам и их статистик по action'ам.
   * @param adOwnerId id владельца рекламных карточек.
   * @return Карту [adId -> stats], где stats - это карта [AdStatAction -> freq:Long].
   */
  def findAdByActionFreqs(adOwnerId: String)(implicit ec: ExecutionContext, client: Client): Future[AdFreqs_t] = {
    val aggName = "aggIdAction"
    prepareSearch
      .setQuery(adOwnerQuery(adOwnerId))
      .setSize(0)
      .addAggregation(
        AggregationBuilders.terms(aggName)
          .script("doc['adId'].value + '.' + doc['action'].value")
      )
      .execute()
      .map { searchResp =>
        searchResp.getAggregations
          .get[Terms](aggName)
          .getBuckets
          .foldLeft[List[(String, (AdStatAction, Long))]] (Nil) { (acc, bucket) =>
            val Array(adId, statActionRaw) = bucket.getKey.split('.')
            val statAction = AdStatActions.withName(statActionRaw)
            val e1 = (adId, (statAction, bucket.getDocCount))
            e1 :: acc
          }
          .groupBy(_._1)
          .mapValues { _.map(_._2).toMap }
      }
  }


  /**
   * Генерим данные для date-гистограммы. Данные эти можно обратить в столбчатую диаграмму или соответствующий график.
   * @param adOwnerIdOpt Возможный id владельца рекламных карточек.
   * @param interval Интервал значений дат.
   * @param actionOpt Возможный id stat-экшена.
   * @param withZeroes Возвращать ли нулевые столбцы? По умолчанию - да.
   * @param dateBoundsOpt Необязательные границы вывода.
   * @return Фьчерс с последовательностью (DateTime, freq:Long) в порядке возрастания даты.
   */
  def dateHistogramFor(adOwnerIdOpt: Option[String], interval: DateHistogram.Interval,
                       dateBoundsOpt: Option[(EsDateTime, EsDateTime)] = None,
                       actionOpt: Option[AdStatAction] = None, withZeroes: Boolean = false
                      )(implicit ec: ExecutionContext, client: Client): Future[DateHistAds_t] = {
    // Собираем поисковый запрос. Надо бы избавится от лишних типов и точек, но почему-то сразу всё становится красным.
    var query: QueryBuilder = adOwnerIdOpt.map[QueryBuilder] { adOwnerId =>
      adOwnerQuery(adOwnerId)
    }.map { adOwnerQuery =>
      actionOpt.fold(adOwnerQuery) { action =>
        QueryBuilders.filteredQuery(
          adOwnerQuery,
          FilterBuilders.termFilter(ACTION_ESFN, action.toString)
        )
      }
    }.orElse {
      actionOpt.map { action =>
        QueryBuilders.termQuery(ACTION_ESFN, action.toString)
      }
    }.getOrElse {
      QueryBuilders.matchAllQuery()
    }
    // Добавить фильтр в query, если задан диапазон интересующих дат.
    if (dateBoundsOpt.isDefined) {
      val (ds, de) = dateBoundsOpt.get
      val dateRangeFilter = FilterBuilders.rangeFilter("timestampRange").gte(ds).lte(de)
      query = QueryBuilders.filteredQuery(query, dateRangeFilter)
    }
    // Собираем и запускаем es-запрос
    val dtHistName = "adDateHist"
    val agg = AggregationBuilders.dateHistogram(dtHistName)
      .field(TIMESTAMP_ESFN)
      .interval(interval)
      .minDocCount(if (withZeroes) 0 else 1)
    if (dateBoundsOpt.isDefined) {
      val Some((ds, de)) = dateBoundsOpt
      agg.extendedBounds(ds, de)
    }
    prepareSearch
      .setQuery(query)
      .setSize(0)
      .addAggregation(agg)
      .execute()
      .map { searchResp =>
        searchResp.getAggregations
          .get[DateHistogram](dtHistName)
          .getBuckets
          .toSeq
          .map { bucket =>
            bucket.getKeyAsDate -> bucket.getDocCount
          }
      }
  }

  def beforeDtQuery(dt: DateTime) = {
    QueryBuilders.rangeQuery(TIMESTAMP_ESFN)
      .from(new DateTime(1970, 1, 1, 0, 0))
      .to(dt)
  }

  /** Подсчёт кол-ва вхождений до указанной даты. */
  def countBefore(dt: DateTime)(implicit ec: ExecutionContext, client: Client): Future[Long] = {
    prepareCount
      .setQuery( beforeDtQuery(dt) )
      .execute()
      .map { _.getCount }
  }

  /** Найти все вхождения до указанной даты. */
  def findBefore(dt: DateTime, maxResults: Int = MAX_RESULTS_DFLT)(implicit ec: ExecutionContext, client: Client): Future[Seq[T]] = {
    prepareSearch
      .setQuery( beforeDtQuery(dt) )
      .setSize(maxResults)
      .addSort(TIMESTAMP_ESFN, SortOrder.ASC)
      .execute()
      .map { searchResp2list }
  }

  /** Удалить все данные до указанной даты. */
  def deleteBefore(dt: DateTime)(implicit ec: ExecutionContext, client: Client): Future[DeleteByQueryResponse] = {
    prepareDeleteByQuery
      .setQuery( beforeDtQuery(dt) )
      .execute()
  }


  /**
   * Десериализация одного элементам модели.
   * @param id id документа.
   * @param m Карта, распарсенное json-тело документа.
   * @return Экземпляр модели.
   */
  override def deserializeOne(id: Option[String], m: collection.Map[String, AnyRef], version: Option[Long]): T = {
    new MAdStat(
      clientAddr  = m.get(CLIENT_ADDR_ESFN).fold("127.0.0.1")(stringParser),
      action      = m.get(ACTION_ESFN).fold(AdStatActions.View) { asaRaw => AdStatActions.withName(stringParser(asaRaw)) },
      adIds       = {
        m.get(AD_ID_ESFN).fold (Seq.empty[String]) {
          case adIdsRaw: jl.Iterable[_] => strListParser(adIdsRaw)
          case other => Seq(stringParser(other))
        }
      },
      adsRendered = m.get(ADS_RENDERED_ESFN).fold(0)(intParser),
      isLocalCl   = m.get(IS_LOCAL_CLIENT_ESFN).fold(false)(booleanParser),
      onNodeIdOpt = m.get(ON_NODE_ID_ESFN).map(stringParser),
      ua          = m.get(UA_ESFN).map(stringParser),
      nodeName    = m.get(NODE_NAME_ESFN).map(stringParser),
      personId    = m.get(PERSON_ID_ESFN).map(stringParser),
      timestamp   = m.get(TIMESTAMP_ESFN).fold(new DateTime(1970,0,0))(dateTimeParser),
      clIpGeo     = m.get(CLIENT_IP_GEO_EFSN).flatMap(GeoPoint.deserializeOpt),
      clTown      = m.get(CLIENT_TOWN_ESFN).map(stringParser),
      clGeoLoc    = m.get(CLIENT_GEO_LOC_ESFN).flatMap(GeoPoint.deserializeOpt),
      clCountry   = m.get(COUNTRY_ESFN).map(stringParser),
      clOSFamily  = m.get(CL_OS_FAMILY_ESFN).map(stringParser),
      clAgent     = m.get(CL_AGENT_ESFN).map(stringParser),
      clDevice    = m.get(CL_DEVICE_ESFN).map(stringParser),
      clickedAdIds = m.get(CLICKED_AD_ID_ESFN).fold(Seq.empty[String])(strListParser),
      generation  = m.get(GENERATION_ESFN).map(longParser),
      id          = id
    )
  }


  /** Статические поля для маппиннга. */
  override def generateMappingStaticFields: List[Field] = List(
    FieldSource(enabled = true),
    FieldAll(enabled = true),
    FieldTtl(enabled = true, default = TTL_DAYS_DFLT + "d")
  )

  /** Маппинги для типа этой модели. */
  override def generateMappingProps: List[DocField] = List(
    FieldString(CLIENT_ADDR_ESFN, index = FieldIndexingVariants.analyzed, include_in_all = true),
    FieldString(ACTION_ESFN, index = FieldIndexingVariants.not_analyzed, include_in_all = false),
    FieldString(UA_ESFN, index = FieldIndexingVariants.analyzed, include_in_all = true),
    FieldString(AD_ID_ESFN, index = FieldIndexingVariants.not_analyzed, include_in_all = false),
    FieldNumber(ADS_RENDERED_ESFN, index = FieldIndexingVariants.analyzed, include_in_all = true, fieldType = DocFieldTypes.integer),
    FieldString(ON_NODE_ID_ESFN, index = FieldIndexingVariants.not_analyzed, include_in_all = false),
    FieldDate(TIMESTAMP_ESFN, index = null, include_in_all = false),
    FieldString(PERSON_ID_ESFN, index = FieldIndexingVariants.not_analyzed, include_in_all = false),
    FieldGeoPoint(CLIENT_IP_GEO_EFSN, geohash = true, geohashPrecision = "5", geohashPrefix = true,
      fieldData = GeoPointFieldData(format = GeoPointFieldDataFormats.compressed, precision = "5km")),
    FieldString(CLIENT_TOWN_ESFN, index = FieldIndexingVariants.analyzed, include_in_all = true),
    FieldGeoPoint(CLIENT_GEO_LOC_ESFN, geohash = true, geohashPrecision = "6", geohashPrefix = true,
      fieldData = GeoPointFieldData(format = GeoPointFieldDataFormats.compressed, precision = "10m")),
    FieldString(NODE_NAME_ESFN, index = FieldIndexingVariants.not_analyzed, include_in_all = true),
    FieldString(COUNTRY_ESFN, index = FieldIndexingVariants.not_analyzed, include_in_all = true),
    FieldBoolean(IS_LOCAL_CLIENT_ESFN, index = FieldIndexingVariants.not_analyzed, include_in_all = false),
    FieldString(CL_OS_FAMILY_ESFN, index = FieldIndexingVariants.not_analyzed, include_in_all = true),
    FieldString(CL_AGENT_ESFN, index = FieldIndexingVariants.not_analyzed, include_in_all = true),
    FieldString(CL_DEVICE_ESFN, index = FieldIndexingVariants.not_analyzed, include_in_all = true),
    FieldString(CLICKED_AD_ID_ESFN, index = FieldIndexingVariants.not_analyzed, include_in_all = false),
    FieldNumber(GENERATION_ESFN, fieldType = DocFieldTypes.long, index = FieldIndexingVariants.analyzed, include_in_all = false)
  )
}

import MAdStat._

class MAdStat(
  val clientAddr  : String,
  val action      : AdStatAction,
  val adIds       : Seq[String],
  val adsRendered : Int,
  val isLocalCl   : Boolean,
  val onNodeIdOpt : Option[String],
  val ua          : Option[String],
  val nodeName    : Option[String]    = None,
  val personId    : Option[String]    = None,
  val timestamp   : DateTime          = new DateTime,
  val clIpGeo     : Option[GeoPoint]  = None,
  val clTown      : Option[String]    = None,
  val clGeoLoc    : Option[GeoPoint]  = None,
  val clCountry   : Option[String]    = None,
  val clOSFamily  : Option[String]    = None,
  val clAgent     : Option[String]    = None,
  val clDevice    : Option[String]    = None,
  val clickedAdIds : Seq[String]      = Nil,
  val generation  : Option[Long]      = None,
  val id          : Option[String]    = None
) extends EsModelT {

  override type T = MAdStat

  @JsonIgnore
  override def companion = MAdStat

  def writeJsonFields(acc: FieldsJsonAcc): FieldsJsonAcc = {
    var acc1: FieldsJsonAcc = CLIENT_ADDR_ESFN -> JsString(clientAddr) ::
      ACTION_ESFN           -> JsString(action.toString) ::
      AD_ID_ESFN            -> JsArray(adIds map JsString.apply) ::
      TIMESTAMP_ESFN        -> date2JsStr(timestamp) ::
      ADS_RENDERED_ESFN     -> JsNumber(adsRendered) ::
      IS_LOCAL_CLIENT_ESFN  -> JsBoolean(isLocalCl) ::
      acc
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
    acc1
  }

  override def versionOpt = None
}


object AdStatActions extends Enumeration {
  type AdStatAction = Value

  val View = Value("v")
  val Click = Value("c")
}


// JMX Содержит кое-какие дополнительные функции.

/** JMX MBean интерфейс */
trait MAdStatJmxMBean extends EsModelJMXMBeanCommon {
  @ManagedOperation(impact = Impact.ACTION)
  @Description("Remove all occurencies BEFORE following timestamp in format: dd.MM.yyyy HH:mm:ss")
  def deleteBefore(dt: String): String

  @ManagedOperation(impact = Impact.INFO)
  @Description("Count all occurencies BEFORE following timestamp in format: dd.MM.yyyy HH:mm:ss")
  def countBefore(dt: String): String

  @ManagedOperation(impact = Impact.INFO)
  @Description("Find all occurenciec BEFORE following timestamp in format: dd.MM.yyyy HH:mm:ss. Second argument sets maxResults returned.")
  def findBefore(dt: String, maxResults: Int): String
}

/** JMX MBean реализация. */
case class MAdStatJmx(implicit val ec: ExecutionContext, val client: Client, val sn: SioNotifierStaticClientI)
  extends EsModelJMXBase with MAdStatJmxMBean {
  import LOGGER._

  def companion = MAdStat

  protected def dtParse(dtStr: String): DateTime = {
    val sdf = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss")
    val d = sdf.parse(dtStr)
    new DateTime(d)
      .withZone(DateTimeZone.forID("Europe/Moscow"))
  }

  override def deleteBefore(dtStr: String): String = {
    warn(s"deleteBefore($dtStr)")
    // Нужно распарсить дату-время из указанных админом.
    try {
      val dt = dtParse(dtStr)
      companion.deleteBefore(dt) map { dbqResp =>
         "OK: " + dbqResp
      }
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
      companion.countBefore(dt) map { count =>
        count + " items before " + dt
      }
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
      companion.findBefore(dt, maxResults) map { results =>
        // Отрендерить результаты в json
        EsModel.toEsJsonDocs(results)
      }
    } catch {
      case ex: Throwable =>
        error(s"Failed to findBefore($dtStr, $maxResults)", ex)
        s"FAIL: findBefore($dtStr, $maxResults) ${ex.getClass.getSimpleName} : ${ex.getMessage}"
    }
  }
}

