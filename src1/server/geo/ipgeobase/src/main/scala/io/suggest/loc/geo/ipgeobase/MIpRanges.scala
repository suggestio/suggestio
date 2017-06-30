package io.suggest.loc.geo.ipgeobase

import com.google.inject.assistedinject.Assisted
import javax.inject.{Inject, Singleton}
import io.suggest.es.model._
import io.suggest.util.logs.MacroLogsImpl
import org.elasticsearch.index.query.QueryBuilders
import play.api.libs.json._
import play.api.libs.functional.syntax._

import scala.collection.Map
import scala.concurrent.{ExecutionContext, Future}

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 05.09.16 13:06
  * Description: Модель диапазонов ip-адресов.
  */

object MIpRange {

  /** Список полей модели. */
  object Fields {
    def COUNTRY_CODE_FN = "cc"
    def IP_RANGE_FN     = "ip"
    def CITY_ID_FN      = "city"
  }

  import Fields._

  /** Поддержка JSON. */
  implicit val FORMAT: OFormat[MIpRange] = (
    (__ \ COUNTRY_CODE_FN).format[String] and
    (__ \ IP_RANGE_FN).format[Seq[String]] and
    (__ \ CITY_ID_FN).formatNullable[CityId_t]
  )(apply, unlift(unapply))

}


/**
  * Недореализация статической стороны модели.
  * Т.к. у нас есть одна модель для создания индекса, а другая для ежедневного использования,
  * то их общий код вынесен в этот класс.
  */
abstract class MIpRangesAbstract
  extends EsModelStatic
    with EsmV2Deserializer
    with EsModelJsonWrites
    with MacroLogsImpl
{

  override type T = MIpRange
  override def ES_TYPE_NAME = "range"

  override protected def esDocReads(meta: IEsDocMeta) = implicitly[Reads[MIpRange]]
  override def esDocWrites = implicitly[OWrites[MIpRange]]

  @deprecated("Use deserializeOne2() instead.", "2016.sep.5")
  override def deserializeOne(id: Option[String], m: Map[String, AnyRef], version: Option[Long]): MIpRange = {
    throw new UnsupportedOperationException("Deprecated API not implemented.")
  }


  import io.suggest.es.util.SioEsUtil._
  import MIpRange.Fields._
  import mCommonDi._


  /** Поиск элементов модели по ip-адресу. */
  def findForIp(ip: String): Future[Seq[MIpRange]] = {
    val fn = IP_RANGE_FN
    val q =QueryBuilders.boolQuery()
      .must {
        QueryBuilders.rangeQuery(fn)
          .lte(ip)
      }
      .must {
        QueryBuilders.rangeQuery(fn)
          .gte(ip)
      }
    val resFut = prepareSearch()
      .setQuery(q)
      .setSize(3)    // Скорее всего тут всегда максимум 1 результат.
      .execute()
      .map(searchResp2list)

    // Залоггировать асинхронный результат, если необходимо.
    if (LOGGER.underlying.isTraceEnabled()) {
      val startedAt = System.currentTimeMillis() - 5L
      resFut.onComplete { tryRes =>
        LOGGER.trace(s"findForId($ip): Result = $tryRes, took ~${System.currentTimeMillis - startedAt} ms.")
      }
    }

    resFut
  }


  override def generateMappingStaticFields: List[Field] = {
    List(
      FieldAll(enabled = false),
      FieldSource(enabled = true)
    )
  }

  override def generateMappingProps: List[DocField] = {
    // Индексируем все поля. ip - чтобы искать, остальное -- для возможности просмотра индекса в kibana.
    List(
      FieldString(COUNTRY_CODE_FN, index = FieldIndexingVariants.not_analyzed, include_in_all = true),
      FieldIp(IP_RANGE_FN, index = FieldIndexingVariants.not_analyzed, include_in_all = true),
      FieldNumber(CITY_ID_FN, fieldType = EsCityIdFieldType, index = FieldIndexingVariants.not_analyzed, include_in_all = true)
    )
  }

}


/** Дефолтовая статическая часть модели для всех повседневных нужд. */
@Singleton
class MIpRanges @Inject() (
  mIndexes                : MIndexes,
  override val mCommonDi  : IEsModelDiVal
)
  extends MIpRangesAbstract
{
  override def ES_INDEX_NAME = mIndexes.INDEX_ALIAS_NAME
}


/** Реализаци статической части модели для периодических нужд взаимодействия с произвольным индексом. */
class MIpRangesTmp @Inject() (
  @Assisted override val ES_INDEX_NAME  : String,
  override val mCommonDi                : IEsModelDiVal
)
  extends MIpRangesAbstract

/** Интерфейс для Guice factory, собирающей инстансы [[MIpRangesTmp]]. */
trait MIpRangesTmpFactory {
  def create(esIndexName: String): MIpRangesTmp
}


/**
  * Класс для всех экземпляров этой модели.
  *
  * @param countryIso2 Код страны (обязателен): RU, FR, etc.
  * @param ipRange Диапазон ip-адресов, описанных списком строк от/до.
  * @param cityId id города, если известен.
  */
case class MIpRange(
  countryIso2 : String,
  ipRange     : Seq[String],
  cityId      : Option[CityId_t]
)
  extends EsModelT
{

  override def versionOpt: Option[Long] = None

  override def id: Option[String] = {
    Some( ipRange.mkString("-") )
  }

}


/** Интерфейс для JMX MBean. */
trait MIpRangesJmxMBean extends EsModelJMXMBeanI {
  def findForIp(ip: String): String
}
/** Реализация jmx mbean [[MIpRangesJmxMBean]]. */
final class MIpRangesJmx @Inject() (
  override val companion    : MIpRanges,
  override implicit val ec  : ExecutionContext
)
  extends EsModelJMXBaseImpl
  with MIpRangesJmxMBean
{

  override type X = MIpRange

  override def findForIp(ip: String): String = {
    val strFut = for (ranges <- companion.findForIp(ip)) yield {
      ranges.mkString("[\n", ",\n", "\n]")
    }
    awaitString(strFut)
  }

}
