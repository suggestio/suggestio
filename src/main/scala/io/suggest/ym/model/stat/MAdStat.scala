package io.suggest.ym.model.stat

import io.suggest.model.{EsModelJMXMBeanCommon, EsModelJMXBase, EsModelT, EsModelStaticT}
import io.suggest.model.EsModel._
import org.joda.time.DateTime
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
import play.api.libs.json.JsString
import io.suggest.util.MacroLogsImpl

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 31.03.14 15:57
 * Description: Для накопления статистики по рекламным карточкам используется эта модель.
 */
object MAdStat extends EsModelStaticT with MacroLogsImpl {

  override type T = MAdStat

  /** Используем изолированный индекс для статистики из-за крайней суровости статистической информации. */
  override val ES_INDEX_NAME = "-siostat"
  val ES_TYPE_NAME = "adStat"

  // Поля модели.
  val CLIENT_ADDR_ESFN = "clientAddr"
  val ACTION_ESFN = "action"
  val UA_ESFN = "ua"
  val AD_ID_ESFN = "adId"
  val AD_OWNER_ID_ESFN = "adOwnerId"
  val TIMESTAMP_ESFN = "timestamp"

  /** Через сколько времени удалять записи статистики. */
  val TTL_DAYS_DFLT = CONFIG.getInt("ad.stat.ttl.period.days") getOrElse 60

  type AdFreqs_t = Map[String, Map[AdStatAction, Long]]
  type DateHistAds_t = Seq[(EsDateTime, Long)]

  protected[model] def adOwnerQuery(adOwnerId: String) = QueryBuilders.termQuery(AD_OWNER_ID_ESFN, adOwnerId)

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


  /** Пустой экземпляр класса. */
  protected def dummy(id: String) = MAdStat(
    id        = Some(id),
    clientAddr = null,
    action    = null,
    ua        = null,
    adId      = null,
    adOwnerId = null,
    timestamp = null,
    personId  = null
  )

  /** Десериализация полей из JSON. */
  def applyKeyValue(acc: MAdStat): PartialFunction[(String, AnyRef), Unit] = {
    case (CLIENT_ADDR_ESFN, value) => acc.clientAddr = stringParser(value)
    case (ACTION_ESFN, value)      => acc.action = AdStatActions.withName(stringParser(value))
    case (UA_ESFN, value)          => acc.ua = Option(stringParser(value))
    case (AD_ID_ESFN, value)       => acc.adId = stringParser(value)
    case (AD_OWNER_ID_ESFN, value) => acc.adOwnerId = stringParser(value)
    case (TIMESTAMP_ESFN, value)   => acc.timestamp = dateTimeParser(value)
    case (PERSON_ID_ESFN, value)   => acc.personId = Option(stringParser(value))
  }


  /** Статические поля для маппиннга. */
  def generateMappingStaticFields: List[Field] = List(
    FieldSource(enabled = true),
    FieldAll(enabled = false),
    FieldTtl(enabled = true, default = TTL_DAYS_DFLT + "d")
  )

  /** Маппинги для типа этой модели. */
  def generateMappingProps: List[DocField] = List(
    FieldString(CLIENT_ADDR_ESFN, index = FieldIndexingVariants.no, include_in_all = true),
    FieldString(ACTION_ESFN, index = FieldIndexingVariants.not_analyzed, include_in_all = false),
    FieldString(UA_ESFN, index = FieldIndexingVariants.no, include_in_all = true),
    FieldString(AD_ID_ESFN, index = FieldIndexingVariants.not_analyzed, include_in_all = false),
    FieldString(AD_OWNER_ID_ESFN, index = FieldIndexingVariants.not_analyzed, include_in_all = false),
    FieldDate(TIMESTAMP_ESFN, index = null, include_in_all = false),
    FieldString(PERSON_ID_ESFN, index = FieldIndexingVariants.not_analyzed, include_in_all = false)
  )
}

import MAdStat._

case class MAdStat(
  var clientAddr: String,
  var action: AdStatAction,
  var adId: String,
  var adOwnerId: String,
  var ua: Option[String],
  var personId: Option[String] = None,
  var timestamp: DateTime = new DateTime,
  var id: Option[String] = None
) extends EsModelT {

  override type T = MAdStat

  @JsonIgnore
  def companion = MAdStat

  def writeJsonFields(acc: FieldsJsonAcc): FieldsJsonAcc = {
    var acc1: FieldsJsonAcc = CLIENT_ADDR_ESFN -> JsString(clientAddr) ::
      ACTION_ESFN -> JsString(action.toString) ::
      AD_ID_ESFN -> JsString(adId) ::
      AD_OWNER_ID_ESFN -> JsString(adOwnerId) ::
      TIMESTAMP_ESFN -> date2JsStr(timestamp) ::
      acc
    if (ua.isDefined)
      acc1 ::= UA_ESFN -> JsString(ua.get)
    if (personId.isDefined)
      acc1 ::= PERSON_ID_ESFN -> JsString(personId.get)
    acc1
  }

}


object AdStatActions extends Enumeration {
  type AdStatAction = Value

  val View = Value("v")
  val Click = Value("c")
}




/** JMX MBean интерфейс */
trait MAdStatJmxMBean extends EsModelJMXMBeanCommon

/** JMX MBean реализация. */
case class MAdStatJmx(implicit val ec: ExecutionContext, val client: Client, val sn: SioNotifierStaticClientI)
  extends EsModelJMXBase with MAdStatJmxMBean {
  def companion = MAdStat
}

