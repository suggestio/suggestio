package models.event

import io.suggest.model.EsModel.{FieldsJsonAcc, stringParser}
import io.suggest.model._
import io.suggest.util.SioEsUtil._
import org.elasticsearch.action.index.IndexRequestBuilder
import org.elasticsearch.client.Client
import org.elasticsearch.index.query.{QueryBuilders, QueryBuilder}
import org.elasticsearch.search.sort.SortOrder
import org.joda.time.DateTime
import play.api.libs.json.{JsBoolean, JsString}
import play.api.Play.{current, configuration}
import util.PlayMacroLogsImpl
import util.event.EventTypes
import EventTypes.EventType
import scala.concurrent.duration._

import scala.collection.Map
import scala.concurrent.{Future, ExecutionContext}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 20.01.15 19:46
 * Description: Модель, описывающая события для узла или другого объекта системы suggest.io.
 */
object MEvent extends EsModelStaticT with PlayMacroLogsImpl {

  override type T = MEvent
  override val ES_TYPE_NAME = "ntf"

  val EVT_TYPE_ESFN     = "et"
  val OWNER_ID_ESFN     = "ownerId"     // Такая же, как в MMartCategory
  val ARGS_ESFN         = "args"
  val DATE_CREATED_ESFN = "dc"
  val IS_CLOSEABLE_ESFN = "ic"
  val IS_UNSEEN_ESFN    = "iu"

  val TTL_DAYS_UNSEEN = configuration.getInt("mevent.ttl.days.unseen") getOrElse 90
  val TTL_DAYS_SEEN   = configuration.getInt("mevent.ttl.days.seen")   getOrElse 30
  
  def isCloseableDflt = true
  def isUnseenDflt    = true
  def dateCreatedDflt = DateTime.now()
  def argsDflt        = EmptyArgsInfo

  override def deserializeOne(id: Option[String], m: Map[String, AnyRef], version: Option[Long]): T = {
    val ntype = Option(m get EVT_TYPE_ESFN)
      .map(stringParser)
      .flatMap(EventTypes.maybeWithName)
      .get
    MEvent(
      etype       = ntype,
      ownerId     = stringParser(m get OWNER_ID_ESFN),
      argsInfo    = ntype.deserializeArgsInfo(m get ARGS_ESFN),
      dateCreated = EsModel.dateTimeParser(m get DATE_CREATED_ESFN),
      isCloseable = Option(m get IS_CLOSEABLE_ESFN).fold(isCloseableDflt)(EsModel.booleanParser),
      isUnseen    = Option(m get IS_UNSEEN_ESFN).fold(isUnseenDflt)(EsModel.booleanParser),
      id          = id,
      versionOpt  = version
    )
  }

  override def generateMappingStaticFields: List[Field] = {
    List(
      FieldSource(enabled = true),
      FieldAll(enabled = true)
    )
  }

  override def generateMappingProps: List[DocField] = {
    List(
      FieldString(EVT_TYPE_ESFN, index = FieldIndexingVariants.not_analyzed, include_in_all = false),
      FieldString(OWNER_ID_ESFN, index = FieldIndexingVariants.not_analyzed, include_in_all = false),
      FieldObject(ARGS_ESFN, properties = Nil, enabled = false),
      FieldDate(DATE_CREATED_ESFN, index = FieldIndexingVariants.analyzed, include_in_all = false),
      FieldBoolean(IS_CLOSEABLE_ESFN, index = FieldIndexingVariants.no, include_in_all = false),
      FieldBoolean(IS_UNSEEN_ESFN, index = FieldIndexingVariants.not_analyzed, include_in_all = false)
    )
  }

  def ownerIdQuery(ownerId: String): QueryBuilder = {
    QueryBuilders.termQuery(OWNER_ID_ESFN, ownerId)
  }

  /**
   * Поиск по ownerId.
   * @param ownerId id владельца.
   * @param limit Макс.кол-во результатов.
   * @param offset Сдвиг.
   * @return Фьючерс со списком результатов, новые сверху.
   */
  def findByOwner(ownerId: String, limit: Int = MAX_RESULTS_DFLT, offset: Int = OFFSET_DFLT)
                  (implicit ec: ExecutionContext, client: Client): Future[Seq[MEvent]] = {
    prepareSearch
      .setQuery( ownerIdQuery(ownerId) )
      .setSize(limit)
      .setFrom(offset)
      .addSort(DATE_CREATED_ESFN, SortOrder.DESC)
      .execute()
      .map { searchResp2list }
  }

}

// TODO Не забыть прилинковать эту модель к SiowebEsModel!

import MEvent._


/** Класс-экземпляр одной нотификации. */
case class MEvent(
  etype         : EventType,
  ownerId       : String,
  argsInfo      : IArgsInfo       = MEvent.argsDflt,
  dateCreated   : DateTime        = MEvent.dateCreatedDflt,
  isCloseable   : Boolean         = MEvent.isCloseableDflt,
  isUnseen      : Boolean         = MEvent.isUnseenDflt,
  ttlDays       : Int             = MEvent.TTL_DAYS_UNSEEN,
  id            : Option[String]  = None,
  versionOpt    : Option[Long]    = None
) extends EsModelT with EsModelPlayJsonT {

  override def companion = MEvent
  override type T = this.type

  override def writeJsonFields(acc: FieldsJsonAcc): FieldsJsonAcc = {
    var acc: FieldsJsonAcc = List(
      EVT_TYPE_ESFN     -> JsString(etype.strId),
      OWNER_ID_ESFN     -> JsString(ownerId),
      DATE_CREATED_ESFN -> EsModel.date2JsStr(dateCreated)
    )
    if (argsInfo.nonEmpty)
      acc ::= ARGS_ESFN -> argsInfo.toPlayJson
    if (isCloseable != isCloseableDflt)
      acc ::= IS_CLOSEABLE_ESFN -> JsBoolean(isCloseable)
    if (isUnseen != isUnseenDflt)
      acc ::= IS_UNSEEN_ESFN -> JsBoolean(isUnseen)
    acc
  }

  /** Генератор indexRequestBuilder'ов. Помогает при построении bulk-реквестов. */
  override def indexRequestBuilder(implicit client: Client): IndexRequestBuilder = {
    super.indexRequestBuilder
      .setTTL( ttlDays.days.toMillis )
  }

}


