package io.suggest.stat.m

import java.text.SimpleDateFormat

import com.google.inject.{Inject, Singleton}
import com.sun.org.glassfish.gmbal.{Description, Impact, ManagedOperation}
import io.suggest.common.empty.EmptyUtil
import io.suggest.model.es._
import io.suggest.util.MacroLogsImpl
import org.elasticsearch.index.query.QueryBuilders
import org.elasticsearch.search.sort.SortOrder
import org.joda.time.{DateTime, DateTimeZone}
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
    (__ \ TIMESTAMP_FN).format[DateTime] and
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


@Singleton
class MStats @Inject() (
  override val mCommonDi  : IEsModelDiVal
)
  extends EsModelStatic
  with MacroLogsImpl
  with EsmV2Deserializer
  with EsModelJsonWrites
{

  import mCommonDi.ec

  override type T = MStat

  override def ES_INDEX_NAME = EsModelUtil.GARBAGE_INDEX
  override def ES_TYPE_NAME  = "stat"


  @deprecated("Use JSON mappers instead", "2016.sep.21")
  override def deserializeOne(id: Option[String], m: Map[String, AnyRef], version: Option[Long]): MStat = {
    throw new UnsupportedOperationException("deprecated API not implemented")
  }


  import io.suggest.util.SioEsUtil._

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
      FieldDate(TIMESTAMP_FN, index = null, include_in_all = false),
      _fieldObject(UA_FN,         MUa),
      _fieldObject(SCREEN_FN,     MScreen),
      _fieldObject(LOCATION_FN,   MLocation),
      _fieldObject(DIAG_FN,       MDiag)
    )
  }

  override protected def esDocReads(meta: IEsDocMeta) = implicitly[Reads[MStat]]
  override def esDocWrites = implicitly[Writes[MStat]]


  import MStat.Fields.TIMESTAMP_FN

  def beforeDtQuery(dt: DateTime) = {
    QueryBuilders.rangeQuery(TIMESTAMP_FN)
      .from(new DateTime(1970, 1, 1, 0, 0))
      .to(dt)
  }

  /** Подсчёт кол-ва вхождений до указанной даты. */
  def countBefore(dt: DateTime): Future[Long] = {
    countByQuery( beforeDtQuery(dt) )
  }

  /** Найти все вхождения до указанной даты. */
  def findBefore(dt: DateTime, maxResults: Int = MAX_RESULTS_DFLT): Future[Seq[T]] = {
    prepareSearch()
      .setQuery( beforeDtQuery(dt) )
      .setSize(maxResults)
      .addSort(TIMESTAMP_FN, SortOrder.ASC)
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

}


/** Инстанс одной статистики. */
case class MStat(
  common            : MCommon,
  actions           : Seq[MAction],
  timestamp         : DateTime        = DateTime.now(),
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
