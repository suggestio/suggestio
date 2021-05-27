package io.suggest.stat.m

import java.time.format.DateTimeFormatter
import java.time.{LocalDateTime, OffsetDateTime, ZoneId}

import com.google.inject.assistedinject.Assisted
import javax.inject.Inject
import io.suggest.common.empty.EmptyUtil
import io.suggest.common.html.HtmlConstants
import io.suggest.es.{IEsMappingProps, MappingDsl}
import io.suggest.es.model._
import io.suggest.util.logs.{MacroLogsImpl, MacroLogsImplLazy}
import org.elasticsearch.index.query.QueryBuilders
import org.elasticsearch.search.aggregations.AggregationBuilders
import org.elasticsearch.search.aggregations.metrics.Max
import org.elasticsearch.search.sort.SortOrder
import play.api.inject.Injector
import play.api.libs.functional.syntax._
import play.api.libs.json._

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
    (__ \ COMMON_FN).format[MCommonStat] and
      //.inmap[MCommon] ( EmptyUtil.opt2ImplMEmptyF(MCommon), EmptyUtil.implEmpty2OptF ) and
    (__ \ ACTIONS_FN).format[Seq[MAction]] and
    (__ \ TIMESTAMP_FN).format[OffsetDateTime] and
    (__ \ UA_FN).formatNullable[MUa]
      .inmap[MUa] ( EmptyUtil.opt2ImplMEmptyF(MUa), EmptyUtil.implEmpty2OptF ) and
    (__ \ SCREEN_FN).formatNullable[MStatScreen]
      .inmap[MStatScreen] ( EmptyUtil.opt2ImplMEmptyF(MStatScreen), EmptyUtil.implEmpty2OptF ) and
    (__ \ LOCATION_FN).formatNullable[MStatLocation]
      .inmap[MStatLocation] ( EmptyUtil.opt2ImplMEmptyF(MStatLocation), EmptyUtil.implEmpty2OptF ) and
    (__ \ DIAG_FN).formatNullable[MDiag]
      .inmap[MDiag] ( EmptyUtil.opt2ImplMEmptyF(MDiag), EmptyUtil.implEmpty2OptF )
  )(apply, unlift(unapply))

}

object MStatsAbstract {

  import MStat.Fields.TIMESTAMP_FN

  def beforeDtQuery(dt: OffsetDateTime) = {
    QueryBuilders.rangeQuery(TIMESTAMP_FN)
      .from( LocalDateTime.of(1970, 1, 1, 0, 0) )
      .to(dt)
  }

}


/** Всякая статическая утиль для MStats-моделей, вынесенная за пределы ООП-наследования. */
final class MStatsModel @Inject()(
                                   injector: Injector,
                                 )
  extends MacroLogsImpl
{
  private lazy val esModel = injector.instanceOf[EsModel]
  implicit private lazy val ec = injector.instanceOf[ExecutionContext]

  import esModel.api._
  import MStat.Fields.TIMESTAMP_FN

  object api {

    implicit class MStatsAbstarctOpsExt(val model: MStatsAbstract) {

      /** Подсчёт кол-ва вхождений до указанной даты. */
      def countBefore(dt: OffsetDateTime): Future[Long] =
        model.countByQuery( MStatsAbstract.beforeDtQuery(dt) )

      /** Найти все вхождения до указанной даты. */
      def findBefore(dt: OffsetDateTime, maxResults: Int = model.MAX_RESULTS_DFLT): Future[Seq[MStat]] = {
        model
          .prepareSearch()
          .setQuery( MStatsAbstract.beforeDtQuery(dt) )
          .setSize(maxResults)
          .addSort(TIMESTAMP_FN, SortOrder.ASC)
          .executeFut()
          .map { model.searchResp2stream }
      }

      /** Удалить все данные до указанной даты. */
      def deleteBefore(dt: OffsetDateTime): Future[Int] = {
        LOGGER.trace(s"deleteBefore($dt): Starting...")
        val scroller = model.startScroll(
          queryOpt          = Some( MStatsAbstract.beforeDtQuery(dt) ),
          resultsPerScroll  = model.BULK_DELETE_QUEUE_LEN / 2
        )
        model.deleteByQuery(scroller)
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
            model
              .prepareSearch()
              .setSize(0)
              .addAggregation {
                AggregationBuilders
                  .max(aggName)
                  .field( TIMESTAMP_FN )
              }
              .executeFut()
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
                LOGGER.debug( s"$logPrefix Agg value dropped as invalid: ${agg.getValue}. This usually means, that all stat indices are empty." )
              isValueValid
            }
            dtStr <- {
              LOGGER.trace( s"$logPrefix Agg result: v=${agg.getValue} vStr=${agg.getValueAsString}" )
              Option( agg.getValueAsString )
            }
          } yield {
            // Тут был код на базе joda-time вида:
            // val dtRes = EsModelUtil.Implicits.jodaDateTimeFormat.reads( JsString(dtStr) )
            val dtRes = implicitly[Reads[OffsetDateTime]].reads( JsString(dtStr) )
            if (dtRes.isError)
              LOGGER.error(s"$logPrefix Failed to parse JSON date: $dtRes")
            dtRes.get
          }
        }
      }

    }
  }
}

/** Абстрактная модель статистики для всеобщих нужд корме обновления индекса. */
sealed abstract class MStatsAbstract
  extends EsModelStatic
  with MacroLogsImplLazy
  with EsmV2Deserializer
  with EsModelJsonWrites
{

  override type T = MStat

  override def ES_TYPE_NAME  = "stat"

  override def indexMapping(implicit dsl: MappingDsl): dsl.IndexMapping = {
    import dsl._
    IndexMapping(
      source = Some( FSource(enabled = someTrue) ),
      properties = Some {
        val F = MStat.Fields

        val objects1 = List[(String, IEsMappingProps)](
          F.COMMON_FN   -> MCommonStat,
          F.UA_FN       -> MUa,
          F.SCREEN_FN   -> MStatScreen,
          F.LOCATION_FN -> MStatLocation,
          F.DIAG_FN     -> MDiag,
        )
          .esSubModelsJsObjects( nested = false )

        val other = Json.obj(
          F.ACTIONS_FN -> FObject.nested(
            enabled     = someTrue,
            properties  = MAction.esMappingProps,
          ),
          F.TIMESTAMP_FN -> FDate.indexedJs,
        )

        objects1 ++ other
      }
    )
  }

  override protected def esDocReads(meta: EsDocMeta) = implicitly[Reads[MStat]]
  override def esDocWrites = implicitly[Writes[MStat]]

  override def withDocMeta(m: MStat, docMeta: EsDocMeta) = m

}


/** Инжектируемая статическая es-модель для всех основных нужд статистики. */
final class MStats extends MStatsAbstract {
  override def ES_INDEX_NAME = MStatIndexes.INDEX_ALIAS_NAME
}


/** Инжектируемая временная статическая модель для нужд ротации stat-индексов. */
final case class MStatsTmp @Inject() (
                                       @Assisted override val ES_INDEX_NAME  : String,
                                     )
  extends MStatsAbstract



/** Инстанс одной статистики. */
final case class MStat(
                        common            : MCommonStat,
                        actions           : Seq[MAction],
                        timestamp         : OffsetDateTime  = OffsetDateTime.now(),
                        ua                : MUa             = MUa.empty,
                        screen            : MStatScreen     = MStatScreen.empty,
                        location          : MStatLocation   = MStatLocation.empty,
                        diag              : MDiag           = MDiag.empty
                      )
  extends EsModelT
{

  // write-only модель, на всякие вторичные мета-поля можно забить.
  override def versioning = EsDocVersion.empty
  override def id         : Option[String]  = None

  override def toString: String = {
    val sb = new StringBuilder(512)

    common.toStringSb(sb)
    sb.append('\n')

    val s = HtmlConstants.SPACE.head

    if (actions.nonEmpty) {
      for (a <- actions) {
        a.toStringSb(sb)
        sb.append(" | ")
      }
      sb.append('\n')
    }

    sb.append(timestamp)
      .append(s)

    if (ua.nonEmpty) {
      ua.toStringSb(sb)
      sb.append(s)
    }

    if (screen.nonEmpty) {
      screen.toStringSb(sb)
      sb.append(s)
    }

    if (location.nonEmpty) {
      location.toStringSb(sb)
      sb.append(s)
    }

    if (diag.nonEmpty) {
      diag.toStringSb(sb)
    }

    sb.toString()
  }

}


/** Интерфейс JMX-модуля модели [[MStats]]. */
trait MStatsJmxMBean extends EsModelJMXMBeanI {

  def deleteBefore(dt: String): String

  def countBefore(dt: String): String

  def findBefore(dt: String, maxResults: Int): String

}

/** Реализация интерфейса [[MStatsJmxMBean]] JMX-модуля модели [[MStats]]. */
final class MStatsJmx @Inject() (
                                  esModel                     : EsModel,
                                  mStatsModel                 : MStatsModel,
                                  override val companion      : MStats,
                                  override val esModelJmxDi   : EsModelJmxDi,
                                )
  extends EsModelJMXBaseImpl
    with MStatsJmxMBean
{

  import esModelJmxDi.ec
  import esModel.api._
  import mStatsModel.api._
  import io.suggest.util.JmxBase._

  override type X = MStat

  protected def dtParse(dtStr: String): OffsetDateTime = {
    val sdf = DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm:ss")
    LocalDateTime.parse(dtStr, sdf)
      .atZone( ZoneId.of("Europe/Moscow") )
      .toOffsetDateTime
  }

  override def deleteBefore(dtStr: String): String = {
    LOGGER.warn(s"deleteBefore($dtStr)")
    // Нужно распарсить дату-время из указанных админом.
    try {
      val dt = dtParse(dtStr)
      val fut = for (countDeleted <- companion.deleteBefore(dt)) yield {
        "OK: count=" + countDeleted
      }
      awaitString(fut)
    } catch {
      case ex: Throwable =>
        LOGGER.error("Unable to parse user-typed date-time: " + dtStr, ex)
        "Failure: user-typed timestamp: " + dtStr + " :: " + ex.getClass.getName + ": " + ex.getMessage
    }
  }

  override def countBefore(dtStr: String): String = {
    LOGGER.trace(s"countBefore($dtStr)")
    try {
      val dt = dtParse(dtStr)
      val fut =
        for (count <- companion.countBefore(dt))
        yield s"$count items before $dt"
      awaitString(fut)

    } catch {
      case ex: Throwable =>
        LOGGER.error("Unable to count items for user-typed dt-str: " + dtStr, ex)
        "Failure: user-typed timestamp: " + dtStr + " :: " + ex.getClass.getName + ": " + ex.getMessage
    }
  }

  override def findBefore(dtStr: String, maxResults: Int): String = {
    LOGGER.trace(s"findBefore($dtStr, $maxResults)")
    try {
      val dt = dtParse(dtStr)
      val fut = for (results <- companion.findBefore(dt, maxResults)) yield {
        // Отрендерить результаты в json
        companion.toEsJsonDocs(results)
      }
      awaitString(fut)

    } catch {
      case ex: Throwable =>
        LOGGER.error(s"Failed to findBefore($dtStr, $maxResults)", ex)
        s"FAIL: findBefore($dtStr, $maxResults) ${ex.getClass.getSimpleName} : ${ex.getMessage}"
    }
  }

}
