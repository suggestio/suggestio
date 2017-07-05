package io.suggest.stat.m

import java.time.format.DateTimeFormatter
import java.time.{LocalDateTime, OffsetDateTime, ZoneId}

import com.google.inject.assistedinject.Assisted
import javax.inject.{Inject, Singleton}
import com.sun.org.glassfish.gmbal.{Description, Impact, ManagedOperation}
import io.suggest.common.empty.EmptyUtil
import io.suggest.es.model._
import io.suggest.util.logs.MacroLogsImpl
import org.elasticsearch.index.query.QueryBuilders
import org.elasticsearch.search.aggregations.AggregationBuilders
import org.elasticsearch.search.aggregations.metrics.max.Max
import org.elasticsearch.search.sort.SortOrder
import play.api.libs.functional.syntax._
import play.api.libs.json._

import scala.collection.Map
import scala.concurrent.{ExecutionContext, Future}

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 21.09.16 10:47
  * Description: Модель статистики, отвязанной от выдачи и на строго новых архитектурах ES-моделей.
  */

object MStat {

  /** Названия корневых полей модели. */
  object Fields {

    /** Имя поля с моделью разных общих полей. */
    val COMMON_FN     = "common"

    /** Экшены. */
    val ACTIONS_FN    = "act"

    /** Имя поля с датой-временем всего этого дела. */
    val TIMESTAMP_FN  = "timestamp"

    /** Поле с моделью данных юзер-агента. */
    val UA_FN         = "ua2"

    /** Имя поля с моделью данных по экрану устройства. */
    val SCREEN_FN     = "screen"

    /** Имя поля с геолокацей и смежными темами. */
    val LOCATION_FN   = "loc"

    /** Имя поля контейнера каких-то диагностических данных. */
    val DIAG_FN       = "diag"

  }


  import Fields._

  /** Поддержка JSON. */
  implicit val FORMAT: OFormat[MStat] = (
    (__ \ COMMON_FN).format[MCommon] and
      //.inmap[MCommon] ( EmptyUtil.opt2ImplMEmptyF(MCommon), EmptyUtil.implEmpty2OptF ) and
    (__ \ ACTIONS_FN).format[Seq[MAction]] and
    (__ \ TIMESTAMP_FN).format[OffsetDateTime] and
    (__ \ UA_FN).formatNullable[MUa]
      .inmap[MUa] ( EmptyUtil.opt2ImplMEmptyF(MUa), EmptyUtil.implEmpty2OptF ) and
    (__ \ SCREEN_FN).formatNullable[MScreen]
      .inmap[MScreen] ( EmptyUtil.opt2ImplMEmptyF(MScreen), EmptyUtil.implEmpty2OptF ) and
    (__ \ LOCATION_FN).formatNullable[MLocation]
      .inmap[MLocation] ( EmptyUtil.opt2ImplMEmptyF(MLocation), EmptyUtil.implEmpty2OptF ) and
    (__ \ DIAG_FN).formatNullable[MDiag]
      .inmap[MDiag] ( EmptyUtil.opt2ImplMEmptyF(MDiag), EmptyUtil.implEmpty2OptF )
  )(apply, unlift(unapply))

}


/** Абстрактная модель статистики для всеобщих нужд корме обновления индекса. */
abstract class MStatsAbstract
  extends EsModelStatic
  with MacroLogsImpl
  with EsmV2Deserializer
  with EsModelJsonWrites
{

  import mCommonDi.ec
  import LOGGER._

  override type T = MStat

  override def ES_TYPE_NAME  = "stat"


  @deprecated("Use JSON mappers instead", "2016.sep.21")
  override def deserializeOne(id: Option[String], m: Map[String, AnyRef], version: Option[Long]): MStat = {
    throw new UnsupportedOperationException("deprecated API not implemented")
  }


  import io.suggest.es.util.SioEsUtil._

  override def generateMappingStaticFields: List[Field] = {
    List(
      FieldAll(enabled = false),
      FieldSource(enabled = true)
    )
  }

  private def _fieldObject(id: String, m: IGenEsMappingProps): FieldObject = {
    FieldObject(id, enabled = true, properties = m.generateMappingProps)
  }

  override def generateMappingProps: List[DocField] = {
    import MStat.Fields._
    List(
      _fieldObject(COMMON_FN,     MCommon),
      FieldNestedObject(ACTIONS_FN, enabled = true, properties = MAction.generateMappingProps),
      FieldDate(TIMESTAMP_FN, index = true, include_in_all = false),
      _fieldObject(UA_FN,         MUa),
      _fieldObject(SCREEN_FN,     MScreen),
      _fieldObject(LOCATION_FN,   MLocation),
      _fieldObject(DIAG_FN,       MDiag)
    )
  }

  override protected def esDocReads(meta: IEsDocMeta) = implicitly[Reads[MStat]]
  override def esDocWrites = implicitly[Writes[MStat]]


  import MStat.Fields.TIMESTAMP_FN

  def beforeDtQuery(dt: OffsetDateTime) = {
    QueryBuilders.rangeQuery(TIMESTAMP_FN)
      .from( LocalDateTime.of(1970, 1, 1, 0, 0) )
      .to(dt)
  }

  /** Подсчёт кол-ва вхождений до указанной даты. */
  def countBefore(dt: OffsetDateTime): Future[Long] = {
    countByQuery( beforeDtQuery(dt) )
  }

  /** Найти все вхождения до указанной даты. */
  def findBefore(dt: OffsetDateTime, maxResults: Int = MAX_RESULTS_DFLT): Future[Seq[T]] = {
    prepareSearch()
      .setQuery( beforeDtQuery(dt) )
      .setSize(maxResults)
      .addSort(TIMESTAMP_FN, SortOrder.ASC)
      .execute()
      .map { searchResp2list }
  }

  /** Удалить все данные до указанной даты. */
  def deleteBefore(dt: OffsetDateTime): Future[Int] = {
    trace(s"deleteBefore($dt): Statring...")
    val scroller = startScroll(
      queryOpt          = Some(beforeDtQuery(dt)),
      resultsPerScroll  = BULK_DELETE_QUEUE_LEN / 2
    )
    deleteByQuery(scroller)
  }

  /**
    * Определить максимальное значение поля timestamp в текущем индексе.
    * Очевидно, используется аггрегация, т.к. это быстро, модно, молодёжно.
    *
    * Метод используется для определения "старости" индекса по последней записи в нём,
    * что удобно для пакетного устаревшей старой статистики путём выявления слишком старых stat-индексов.
    *
    * @return Фьючерс с опциональной датой. Если там None, наверное в индексе нет данных вообще.
    */
  def findMaxTimestamp(): Future[Option[OffsetDateTime]] = {
    val aggName = "dtMax"
    for {
      resp <- {
        prepareSearch()
          .setSize(0)
          .addAggregation {
            AggregationBuilders
              .max(aggName)
              .field(TIMESTAMP_FN)
          }
          .execute()
      }
    } yield {
      /*
       * Agg-результат имеет вот такой вид:
       *   null
       * или же
       *   {
       *     "value" : 1.473860569481E12,
       *     "value_as_string" : "2016-09-14T13:42:49.481Z"
       *   }
       */
      lazy val logPrefix = s"findMaxTimestamp(${System.currentTimeMillis}):"
      for {
        agg   <- Option( resp.getAggregations.get[Max](aggName) )
        // Если 0 записей статистики, то результат всё равно не пустой. Будет что-то типа:
        // Agg result: v=-Infinity vStr=-292275055-05-16T16:47:04.192Z
        if {
          val isValueValid = agg.getValue > 1000 && agg.getValue < Double.PositiveInfinity
          if (!isValueValid)
            debug( s"$logPrefix Agg value dropped as invalid: ${agg.getValue}. This usually means, that all stat indices are empty." )
          isValueValid
        }
        dtStr <- {
          trace( s"$logPrefix Agg result: v=${agg.getValue} vStr=${agg.getValueAsString}" )
          Option( agg.getValueAsString )
        }
      } yield {
        // Тут был код на базе joda-time вида:
        // val dtRes = EsModelUtil.Implicits.jodaDateTimeFormat.reads( JsString(dtStr) )
        val dtRes = implicitly[Reads[OffsetDateTime]].reads( JsString(dtStr) )
        if (dtRes.isError)
          error(s"$logPrefix Failed to parse JSON date: $dtRes")
        dtRes.get
      }
    }
  }

}


/** Инжектируемая статическая es-модель для всех основных нужд статистики. */
@Singleton
class MStats @Inject() (
  mStatIndexes            : MStatIndexes,
  override val mCommonDi  : IEsModelDiVal
)
  extends MStatsAbstract
{
  override def ES_INDEX_NAME = mStatIndexes.INDEX_ALIAS_NAME
}


/** Инжектируемая временная статическая модель для нужд ротации stat-индексов. */
class MStatsTmp @Inject() (
  @Assisted override val ES_INDEX_NAME  : String,
  override val mCommonDi                : IEsModelDiVal
)
  extends MStatsAbstract

/** Интерфейс для guice-factory, возвращающей инстансы [[MStatsTmp]]. */
trait MStatsTmpFactory {
  def create(esIndexName: String): MStatsTmp
}



/** Инстанс одной статистики. */
case class MStat(
  common            : MCommon,
  actions           : Seq[MAction],
  timestamp         : OffsetDateTime  = OffsetDateTime.now(),
  ua                : MUa             = MUa.empty,
  screen            : MScreen         = MScreen.empty,
  location          : MLocation       = MLocation.empty,
  diag              : MDiag           = MDiag.empty
)
  extends EsModelT
{

  // write-only модель, на всякие вторичные мета-поля можно забить.
  override def versionOpt : Option[Long]    = None
  override def id         : Option[String]  = None

}


/** Интерфейс JMX-модуля модели [[MStats]]. */
trait MStatsJmxMBean extends EsModelJMXMBeanI {

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

/** Реализация интерфейса [[MStatsJmxMBean]] JMX-модуля модели [[MStats]]. */
final class MStatsJmx @Inject() (
  override val companion    : MStats,
  override implicit val ec  : ExecutionContext
)
  extends EsModelJMXBaseImpl
    with MStatsJmxMBean
{

  override type X = MStat

  import LOGGER._

  protected def dtParse(dtStr: String): OffsetDateTime = {
    val sdf = DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm:ss")
    LocalDateTime.parse(dtStr, sdf)
      .atZone( ZoneId.of("Europe/Moscow") )
      .toOffsetDateTime
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
