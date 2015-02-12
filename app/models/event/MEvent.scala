package models.event

import search.IEventsSearchArgs
import io.suggest.event.SioNotifier.{Classifier, Event}
import io.suggest.event.SioNotifierStaticClientI
import io.suggest.model.EsModel.{FieldsJsonAcc, stringParser}
import io.suggest.model._
import io.suggest.util.SioEsUtil._
import io.suggest.ym.model.common.EsDynSearchStatic
import org.elasticsearch.action.index.IndexRequestBuilder
import org.elasticsearch.client.Client
import org.joda.time.DateTime
import play.api.libs.json.{Json, JsBoolean, JsString}
import play.api.Play.{current, configuration}
import util.PlayMacroLogsImpl
import util.event.EventTypes
import EventTypes.EventType
import scala.concurrent.duration._

import scala.collection.Map
import scala.concurrent.ExecutionContext

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 20.01.15 19:46
 * Description: Модель, описывающая события для узла или другого объекта системы suggest.io.
 *
 * 2015.feb.12: Поле isUnseen работает по значениям: true или missing. Это позволяет разгружать индекс поля, когда
 * сообщения прочитаны (а таких большинство, и искать по этому значение не требуется).
 */
object MEvent extends EsModelStaticT with PlayMacroLogsImpl with EsDynSearchStatic[IEventsSearchArgs] {

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
  def dateCreatedDflt = DateTime.now()
  def argsDflt        = EmptyArgsInfo

  override def deserializeOne(id: Option[String], m: Map[String, AnyRef], version: Option[Long]): T = {
    MEvent(
      etype       = m.get(EVT_TYPE_ESFN)
        .map(stringParser)
        .flatMap(EventTypes.maybeWithName)
        .get,
      ownerId     = m.get(OWNER_ID_ESFN)
        .map(stringParser)
        .get,
      argsInfo    = m.get(ARGS_ESFN)
        .fold [ArgsInfo] (EmptyArgsInfo) (ArgsInfo.fromJacksonJson),
      dateCreated = m.get(DATE_CREATED_ESFN)
        .fold(DateTime.now)(EsModel.dateTimeParser),
      isCloseable = m.get(IS_CLOSEABLE_ESFN)
        .fold(isCloseableDflt)(EsModel.booleanParser),
      isUnseen    = m.get(IS_UNSEEN_ESFN)
        .fold(false)(EsModel.booleanParser),
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
      FieldObject(ARGS_ESFN, enabled = false, properties = ArgsInfo.generateMappingProps),
      FieldDate(DATE_CREATED_ESFN, index = FieldIndexingVariants.analyzed, include_in_all = false),
      FieldBoolean(IS_CLOSEABLE_ESFN, index = FieldIndexingVariants.no, include_in_all = false),
      FieldBoolean(IS_UNSEEN_ESFN, index = FieldIndexingVariants.not_analyzed, include_in_all = false)
    )
  }

  /**
   * Сборка event classifier для простоты взаимодействия с SioNotifier.
   * @param etype Тип события, если нужен.
   * @param ownerId id владельца, если требуется.
   * @param argsInfo Экземпляр [[ArgsInfo]], если есть.
   * @return Classifier.
   */
  def getClassifier(etype: Option[EventType], ownerId: Option[String], argsInfo: ArgsInfo = EmptyArgsInfo): Classifier = {
    Some(classOf[T].getSimpleName) :: etype :: ownerId :: argsInfo.getClassifier
  }

}


import MEvent._


/** Класс-экземпляр одной нотификации. */
case class MEvent(
  etype         : EventType,
  ownerId       : String,
  argsInfo      : ArgsInfo        = MEvent.argsDflt,
  dateCreated   : DateTime        = MEvent.dateCreatedDflt,
  isCloseable   : Boolean         = MEvent.isCloseableDflt,
  isUnseen      : Boolean         = true,
  ttlDays       : Option[Int]     = Some(MEvent.TTL_DAYS_UNSEEN),
  id            : Option[String]  = None,
  versionOpt    : Option[Long]    = None
) extends EsModelT with EsModelPlayJsonT with IMEvent {

  override def companion = MEvent
  override type T = this.type

  override def writeJsonFields(acc: FieldsJsonAcc): FieldsJsonAcc = {
    var acc: FieldsJsonAcc = List(
      EVT_TYPE_ESFN     -> JsString(etype.strId),
      OWNER_ID_ESFN     -> JsString(ownerId),
      DATE_CREATED_ESFN -> EsModel.date2JsStr(dateCreated)
    )
    if (isUnseen)
      acc ::= IS_UNSEEN_ESFN -> JsBoolean(isUnseen)
    if (argsInfo.nonEmpty)
      acc ::= ARGS_ESFN -> Json.toJson(argsInfo)
    if (isCloseable != isCloseableDflt)
      acc ::= IS_CLOSEABLE_ESFN -> JsBoolean(isCloseable)
    acc
  }

  /** Генератор indexRequestBuilder'ов. Помогает при построении bulk-реквестов. */
  override def indexRequestBuilder(implicit client: Client): IndexRequestBuilder = {
    val irb = super.indexRequestBuilder
    if (ttlDays.isDefined)
      irb.setTTL( ttlDays.get.days.toMillis )
    irb
  }

}


trait MEventJmxMBean extends EsModelJMXMBeanI
final class MEventJmx(implicit val ec: ExecutionContext, val client: Client, val sn: SioNotifierStaticClientI)
  extends EsModelJMXBase
  with MEventJmxMBean
{
  override def companion = MEvent
}


/** Минимальный интерфейс абстрактного события.
  * Используется для рендера шаблонов, аргументы которых абстрагированы от конкретной реализации. */
trait IEvent extends OptStrId with Event {
  def etype         : EventType
  def ownerId       : String
  def argsInfo      : ArgsInfo
  def dateCreated   : DateTime
  def isCloseable   : Boolean
  def isUnseen      : Boolean
}

/** Частичная реализация [[IEvent]] с реализацией getClassifier() в рамках текущей модели. */
trait IMEvent extends IEvent {
  override def getClassifier: Classifier = {
    MEvent.getClassifier(Some(etype), Some(ownerId), argsInfo)
  }
}


/**
 * Реализация [[IEvent]] для нехранимого события, т.е. когда что-то нужно отрендерить в режиме БЫСТРАБЛДЖАД!
 * @param etype Тип события.
 * @param ownerId id owner'а.
 * @param argsInfo Необязательная инфа по параметрам [[EmptyArgsInfo]].
 * @param dateCreated Дата создания [now].
 * @param isCloseable Закрывабельность [false].
 * @param isUnseen Юзер в первый раз видит событие? [конечно true].
 */
case class MEventTmp(
  etype       : EventType,
  ownerId     : String,
  argsInfo    : ArgsInfo        = EmptyArgsInfo,
  dateCreated : DateTime        = DateTime.now(),
  isCloseable : Boolean         = false,
  isUnseen    : Boolean         = true,
  id          : Option[String]  = None
) extends IMEvent


