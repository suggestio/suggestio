package io.suggest.ym.model.stat

import io.suggest.model.{EsModelT, EsModelStaticT}
import io.suggest.model.EsModel._
import org.joda.time.DateTime
import com.fasterxml.jackson.annotation.JsonIgnore
import org.elasticsearch.common.xcontent.XContentBuilder
import io.suggest.util.SioEsUtil._
import io.suggest.util.MyConfig.CONFIG
import scala.concurrent.{Future, ExecutionContext}
import org.elasticsearch.client.Client
import org.elasticsearch.index.query.QueryBuilders
import org.elasticsearch.search.aggregations.AggregationBuilders
import scala.collection.JavaConversions._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 31.03.14 15:57
 * Description: Для накопления статистики по рекламным карточкам используется эта модель.
 */
object MAdStat extends EsModelStaticT[MAdStat] {
  val ES_TYPE_NAME: String = "adStat"

  // Поля модели.
  val CLIENT_ADDR_ESFN = "clientAddr"
  val ACTION_ESFN = "action"
  val UA_ESFN = "ua"
  val AD_ID_ESFN = "adId"
  val AD_OWNER_ID_ESFN = "adOwnerId"
  val TIMESTAMP_ESFN = "timestamp"

  /** Через сколько времени удалять записи статистики. */
  val TTL_DFLT = CONFIG.getString("ad.stat.ttl.period") getOrElse "60d"

  /**
   * Аггрегат, порождающий карту из id реклам и их статистик по action'ам.
   * @param adOwnerId id владельца рекламных карточек.
   * @return Карту [adId -> stats], где stat - это карта [AdStatAction -> freq].
   */
  def aggForAdOwnerPerAd(adOwnerId: String)(implicit ec: ExecutionContext, client: Client): Future[Map[String, Map[AdStatAction, Int]]] = {
    client.prepareSearch(ES_INDEX_NAME)
      .setTypes(ES_TYPE_NAME)
      .setQuery(QueryBuilders.termQuery(AD_OWNER_ID_ESFN, adOwnerId))
      .addAggregation(
        AggregationBuilders.terms("aggId").field(AD_ID_ESFN).subAggregation(
          AggregationBuilders.terms("aggAction").field(ACTION_ESFN)
        )
      )
      .execute()
      .map { searchResp =>
        searchResp.getAggregations.getAsMap.toMap.mapValues { aggr =>
          ???
        }
        ???
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
    FieldTtl(enabled = true, default = TTL_DFLT)
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
) extends EsModelT[MAdStat] {

  @JsonIgnore
  def companion = MAdStat

  def writeJsonFields(acc: XContentBuilder) {
    acc.field(CLIENT_ADDR_ESFN, clientAddr)
      .field(ACTION_ESFN, action.toString)
      .field(UA_ESFN, ua)
      .field(AD_ID_ESFN, adId)
      .field(AD_OWNER_ID_ESFN, adOwnerId)
      .field(TIMESTAMP_ESFN, timestamp)
    if (personId.isDefined)
      acc.field(PERSON_ID_ESFN, personId.get)
  }
}


object AdStatActions extends Enumeration {
  type AdStatAction = Value

  val View = Value("v")
  val Click = Value("c")
}

