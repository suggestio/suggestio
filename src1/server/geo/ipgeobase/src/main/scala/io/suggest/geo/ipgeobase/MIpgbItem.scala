package io.suggest.geo.ipgeobase

import io.suggest.common.empty.EmptyUtil
import io.suggest.es.{IEsMappingProps, MappingDsl}
import io.suggest.es.model.{EsDocMeta, EsDocVersion, EsModel, EsModelJMXBaseImpl, EsModelJMXMBeanI, EsModelJsonWrites, EsModelStatic, EsModelT, EsmV2Deserializer}
import io.suggest.geo.MGeoPoint
import io.suggest.util.JmxBase
import io.suggest.util.logs.{MacroLogsImpl, MacroLogsImplLazy}
import japgolly.univeq._
import org.elasticsearch.index.query.QueryBuilders
import play.api.inject.Injector
import play.api.libs.json._
import play.api.libs.functional.syntax._

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 05.09.16 13:06
  * Description: Model for storing and searching IPGeoBase elements: ip-ranges, cities (and possibly - others).
  * Elasticsearch 6+ dropped `_type` field, so everything in index must be unified in the only type or separated explicitly.
  */

object MIpgbItem {

  /** Fields forwarded to Payload-model. */
  @inline def Fields = MIpgbItemPayload.Fields


  /** Make an IP-Range [[MIpgbItem]] instance. */
  def ipRange(
               countryIso2     : String,
               ipRange         : Seq[String],
               cityId          : Option[CityId_t],
             ): MIpgbItem = {
    apply(
      payload = MIpgbItemPayload(
        itemType        = MIpgbItemTypes.IpRange,
        countryIso2     = Some( countryIso2 ),
        ipRange         = ipRange,
        cityId          = cityId,
      ),
      id = Some( ipRange.mkString("-") ),
    )
  }

  /** Make City [[MIpgbItem]] instance. */
  def city(
            cityId    : CityId_t,
            cityName  : Option[String],
            region    : Option[String],
            center    : MGeoPoint,
          ): MIpgbItem = {
    MIpgbItem(
      payload = MIpgbItemPayload(
        cityId    = Some(cityId),
        itemType  = MIpgbItemTypes.City,
        cityName  = cityName,
        region    = region,
        center    = Some(center),
      ),
      id = Some {
        cityId2esId( cityId )
      },
    )

  }

  def cityId2esId(cityId: CityId_t): String = {
    cityId.toString
  }

}


/** ES-document model for [[MIpgbItemPayload]]-content. Contains payload and ES-metadata.
  *
  * @param payload Data.
  * @param id ES _id.
  * @param versioning ES _version.
  */
final case class MIpgbItem(
                            payload                   : MIpgbItemPayload,
                            override val id           : Option[String],
                            override val versioning   : EsDocVersion            = EsDocVersion.empty,
                          )
  extends EsModelT


/** Static EsModel for [[MIpgbItem]]s storage access.
  * No injection here. All dependency-related must be placed into [[MIpgbItemsModel]]. */
final case class MIpgbItems (
                              override val ES_INDEX_NAME  : String,
                            )
  extends EsModelStatic
  with EsmV2Deserializer
  with EsModelJsonWrites
  with MacroLogsImpl
{

  override type T = MIpgbItem

  override def ES_TYPE_NAME = "items"

  override protected def esDocReads(meta: EsDocMeta): Reads[MIpgbItem] = {
    implicitly[Reads[MIpgbItemPayload]]
      .map[MIpgbItem] {
        MIpgbItem(_, meta.id, meta.version)
      }
  }

  override def esDocWrites: OWrites[MIpgbItem] = {
    implicitly[OWrites[MIpgbItemPayload]]
      .contramap[MIpgbItem](_.payload)
  }

  override def indexMapping(implicit dsl: MappingDsl): dsl.IndexMapping = {
    import dsl._
    IndexMapping(
      source = Some( FSource(enabled = someTrue) ),
      properties = Some {
        MIpgbItemPayload.esMappingProps
      }
    )
  }

  override def withDocMeta(m: MIpgbItem, docMeta: EsDocMeta): MIpgbItem =
    m.copy(id = docMeta.id, versioning = docMeta.version)

}
object MIpgbItems {
  /** Current ready-to use ipgeobase index. */
  def CURRENT = apply( MIndexes.INDEX_ALIAS_NAME )
}


/** Extended injected implicit API for EsModel implementation [[MIpgbItems]]. */
final class MIpgbItemsModel @Inject()(
                                       injector: Injector
                                     )
  extends MacroLogsImplLazy
{

  private lazy val esModel = injector.instanceOf[EsModel]
  implicit private lazy val ec = injector.instanceOf[ExecutionContext]


  object api {

    implicit class MIpRangesOps( model: MIpgbItems ) {

      /** Search for elements using ip-address. */
      def findForIp(ip: String): Future[Seq[MIpgbItem]] = {
        import esModel.api._

        val fn = MIpgbItem.Fields.IP_RANGE_FN
        val q = QueryBuilders.boolQuery()
          .must {
            QueryBuilders.rangeQuery(fn)
              .lte(ip)
          }
          .must {
            QueryBuilders.rangeQuery(fn)
              .gte(ip)
          }
          .filter {
            QueryBuilders.termQuery( MIpgbItem.Fields.ITEM_TYPE, MIpgbItemTypes.IpRange.value )
          }

        val resFut = model
          .prepareSearch()
          .setQuery(q)
          .setSize(3)    // Скорее всего тут всегда максимум 1 результат.
          .executeFut()
          .map( model.searchResp2stream )

        // Залоггировать асинхронный результат, если необходимо.
        if (LOGGER.underlying.isTraceEnabled()) {
          val startedAt = System.currentTimeMillis() - 5L
          resFut.onComplete { tryRes =>
            LOGGER.trace(s"findForId($ip): Result = $tryRes, took ~${System.currentTimeMillis - startedAt} ms.")
          }
        }

        resFut
      }

      /** Get by city id.
        * Internally, getById() is used, because city-items are indexed by id.
        *
        * @param cityId ipgb id of city.
        * @return Future with [[MIpgbItem]] optionally found.
        */
      def getByCityId(cityId: CityId_t): Future[Option[MIpgbItem]] = {
        import esModel.api._

        model
          .getById( MIpgbItem.cityId2esId(cityId) )
          // Filter by ipgb type. Usually useless, because _id must be enought.
          .map { ipgbItemOpt =>
            ipgbItemOpt.filter { ipgbItem =>
              var r = ipgbItem.payload.itemType ==* MIpgbItemTypes.City
              if (!r) LOGGER.warn( s"getByCityId($cityId): Unexpected item#${ipgbItem.id} type[${ipgbItem.payload.itemType}], ${MIpgbItemTypes.City} type expected\n $ipgbItem" )
              r
            }
          }
      }

    }
  }

}


/** Payload model used for explicit separation between ES doc metadata (_id, _version fields) and document payload. */
object MIpgbItemPayload
  extends IEsMappingProps
{

  /** ElasticSearch-side field names of [[MIpgbItem]]. */
  object Fields {

    // Mandatory fields:
    final def ITEM_TYPE             = "itemType"

    // Fields for ip-range or city items:
    final def CITY_ID_FN            = "cityId"

    // Fields for ip-range items:
    final def COUNTRY_CODE_FN       = "countryCode"
    final def IP_RANGE_FN           = "ipRange"

    // Fields for city items:
    final def CITY_NAME_FN          = "cityName"
    final def REGION_FN             = "region"
    final def CENTER_FN             = "center"

  }


  /** JSON support. */
  implicit def ipgbItemPayloadJson: OFormat[MIpgbItemPayload] = {
    val F = Fields
    (
      (__ \ F.ITEM_TYPE).format[MIpgbItemType] and
      (__ \ F.CITY_ID_FN).formatNullable[CityId_t] and
      (__ \ F.COUNTRY_CODE_FN).formatNullable[String] and
      (__ \ F.IP_RANGE_FN).formatNullable[Seq[String]]
        .inmap[Seq[String]](
          EmptyUtil.opt2ImplEmptyF( Nil ),
          ipRanges => Option.when( ipRanges.nonEmpty )(ipRanges)
        ) and
      (__ \ F.CITY_NAME_FN).formatNullable[String] and
      (__ \ F.REGION_FN).formatNullable[String] and
      (__ \ F.CENTER_FN).formatNullable[MGeoPoint]
    )(apply, unlift(unapply))
  }


  @inline implicit def univEq: UnivEq[MIpgbItemPayload] = UnivEq.derive


  override def esMappingProps(implicit dsl: MappingDsl): JsObject = {
    import dsl._
    val F = Fields

    Json.obj(
      F.ITEM_TYPE -> FKeyWord(
        index = someTrue,
      ),
      F.CITY_ID_FN -> FNumber(
        typ   = DocFieldTypes.Short,
        index = someTrue,
      ),
      F.COUNTRY_CODE_FN -> FKeyWord.indexedJs,
      F.IP_RANGE_FN -> FIp.indexedJs,
      F.CITY_NAME_FN -> FText.notIndexedJs,
      F.REGION_FN -> FText.notIndexedJs,
      F.CENTER_FN -> FGeoPoint.indexedJs,
    )
  }

}


/** Container for mixed IpRange+City model.
  *
  * @param itemType Type of item (~replacement for deprecated _type field in ES-6.x+). City or IpRange.
  * @param cityId City id for city and IpRange.
  * @param countryIso2 Country for ip-range.
  * @param ipRange IP Addresses range.
  * @param cityName Name of city.
  * @param region Region of city.
  * @param center Geo.point (center of mean.geolocation).
  */
final case class MIpgbItemPayload(
                                   itemType                  : MIpgbItemType,
                                   // ip-range & city
                                   cityId                    : Option[CityId_t]        = None,
                                   // ip-range
                                   countryIso2               : Option[String]          = None,
                                   ipRange                   : Seq[String]             = Nil,
                                   // city:
                                   cityName                  : Option[String]          = None,
                                   region                    : Option[String]          = None,
                                   center                    : Option[MGeoPoint]       = None,
                                 )


/** JMX MBean interface for [[MIpgbItem]] ES-model. */
trait MIpgbItemsJmxMBean extends EsModelJMXMBeanI {

  def getByCityId(cityId: CityId_t): String

  def findForIp(ip: String): String

}
/** MBean implementatiopn for [[MIpgbItemsJmxMBean]]. */
final class MIpgbItemsJmx @Inject() (
                                     override val injector: Injector,
                                   )
  extends EsModelJMXBaseImpl
  with MIpgbItemsJmxMBean
{

  override type X = MIpgbItem

  private def mIpgbItemModel = injector.instanceOf[MIpgbItemsModel]
  override def companion = injector.instanceOf[MIpgbItems]


  override def getByCityId(cityId: CityId_t): String = {
    val m = mIpgbItemModel
    import m.api._

    val strFut = for {
      mCityOpt <- companion.getByCityId(cityId)
    } yield {
      mCityOpt.toString
    }
    JmxBase.awaitString(strFut)
  }


  override def findForIp(ip: String): String = {
    val m = mIpgbItemModel
    import m.api._

    val strFut = for (ranges <- companion.findForIp(ip)) yield {
      ranges.mkString("[\n", ",\n", "\n]")
    }
    JmxBase.awaitString(strFut)
  }

}
