package io.suggest.geo.ipgeobase

import com.google.inject.assistedinject.Assisted
import io.suggest.es.MappingDsl
import javax.inject.{Inject, Singleton}
import io.suggest.es.model._
import io.suggest.util.logs.MacroLogsImpl
import org.elasticsearch.index.query.QueryBuilders
import play.api.libs.json._
import play.api.libs.functional.syntax._

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

@Singleton
class MIpRangesModel @Inject()(esModel      : EsModel)
                              (implicit ec  : ExecutionContext)
  extends MacroLogsImpl
{

  import esModel.api._
  import MIpRange.Fields._

  object api {

    implicit class MIpRangesOps( model: MIpRangesAbstract ) {

      /** Поиск элементов модели по ip-адресу. */
      def findForIp(ip: String): Future[Seq[MIpRange]] = {
        val fn = IP_RANGE_FN
        val q = QueryBuilders.boolQuery()
          .must {
            QueryBuilders.rangeQuery(fn)
              .lte(ip)
          }
          .must {
            QueryBuilders.rangeQuery(fn)
              .gte(ip)
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

    }
  }

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

  /** Сборка маппинга индекса по новому формату. */
  override def indexMapping(implicit dsl: MappingDsl): dsl.IndexMapping = {
    import dsl._
    IndexMapping(
      source = Some( FSource(enabled = someTrue) ),
      properties = Some {
        val F = MIpRange.Fields
        Json.obj(
          F.COUNTRY_CODE_FN -> FKeyWord.indexedJs,
          F.IP_RANGE_FN     -> FIp.indexedJs,
          F.CITY_ID_FN      -> FNumber(
            typ   = DocFieldTypes.Short,
            index = someTrue,
          ),
        )
      }
    )
  }

}


/** Дефолтовая статическая часть модели для всех повседневных нужд. */
@Singleton
final class MIpRanges extends MIpRangesAbstract {
  override def ES_INDEX_NAME = MIndexes.INDEX_ALIAS_NAME
}


/** Реализаци статической части модели для периодических нужд взаимодействия с произвольным индексом. */
class MIpRangesTmp @Inject() (
  @Assisted override val ES_INDEX_NAME  : String,
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
                                     mIpRangesModel              : MIpRangesModel,
                                     override val companion      : MIpRanges,
                                     override val esModelJmxDi   : EsModelJmxDi,
                                   )
  extends EsModelJMXBaseImpl
  with MIpRangesJmxMBean
{

  import esModelJmxDi.ec
  import mIpRangesModel.api._
  import io.suggest.util.JmxBase._

  override type X = MIpRange

  override def findForIp(ip: String): String = {
    val strFut = for (ranges <- companion.findForIp(ip)) yield {
      ranges.mkString("[\n", ",\n", "\n]")
    }
    awaitString(strFut)
  }

}
