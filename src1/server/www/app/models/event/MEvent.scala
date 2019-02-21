package models.event

import java.time.OffsetDateTime

import io.suggest.event.SioNotifier.{Classifier, Event}
import io.suggest.util.JacksonParsing.FieldsJsonAcc
import io.suggest.es.util.SioEsUtil._
import play.api.libs.json._
import play.api.libs.functional.syntax._
import javax.inject.{Inject, Singleton}
import io.suggest.es.model._
import io.suggest.primo.id.OptStrId
import io.suggest.util.JacksonParsing
import io.suggest.util.logs.MacroLogsImpl
import io.suggest.common.empty.OptionUtil.BoolOptOps

import scala.collection.Map

// TODO Модель оставлена тут только для совместимости с legacy-кодом из lk-adv-ext.

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 20.01.15 19:46
 * Description: Модель, описывающая события для узла или другого объекта системы suggest.io.
 *
 * 2015.feb.12: Поле isUnseen работает по значениям: true или missing. Это позволяет разгружать индекс поля, когда
 * сообщения прочитаны (а таких большинство, и искать по этому значение не требуется).
 */
object MEvent {

  val EVT_TYPE_ESFN     = "et"
  val OWNER_ID_ESFN     = "ownerId"
  val ARGS_ESFN         = "args"
  val DATE_CREATED_ESFN = "dc"
  val IS_CLOSEABLE_ESFN = "ic"
  val IS_UNSEEN_ESFN    = "iu"

  def TTL_DAYS_UNSEEN = 90
  def TTL_DAYS_SEEN   = 30

  def isCloseableDflt = true
  def dateCreatedDflt = OffsetDateTime.now()
  def argsDflt        = EmptyArgsInfo


  /**
   * Сборка event classifier для простоты взаимодействия с SioNotifier.
   *
   * @param etype Тип события, если нужен.
   * @param ownerId id владельца, если требуется.
   * @param argsInfo Экземпляр [[ArgsInfo]], если есть.
   * @return Classifier.
   */
  def getClassifier(etype: Option[MEventType], ownerId: Option[String], argsInfo: ArgsInfo = EmptyArgsInfo): Classifier = {
    Some(classOf[MEvent].getSimpleName) ::
      etype ::
      ownerId ::
      argsInfo.getClassifier
  }

}


@Singleton
class MEvents
  extends EsModelStatic
    with MacroLogsImpl
    with EsmV2Deserializer
    with EsModelPlayJsonStaticT
{

  import MEvent._

  override type T = MEvent
  override val ES_TYPE_NAME = "ntf"


  override def generateMappingStaticFields: List[Field] = {
    List(
      FieldSource(enabled = true),
      FieldAll(enabled = true)
    )
  }

  override def generateMappingProps: List[DocField] = {
    List(
      FieldKeyword(EVT_TYPE_ESFN, index = true, include_in_all = false),
      FieldKeyword(OWNER_ID_ESFN, index = true, include_in_all = false),
      FieldObject(ARGS_ESFN, enabled = false, properties = ArgsInfo.generateMappingProps),
      FieldDate(DATE_CREATED_ESFN, index = true, include_in_all = false),
      FieldBoolean(IS_CLOSEABLE_ESFN, index = false, include_in_all = false),
      FieldBoolean(IS_UNSEEN_ESFN, index = true, include_in_all = false)
    )
  }

  @deprecated("Delete it, v2 is ready here", "2015.sep.07")
  override def deserializeOne(id: Option[String], m: Map[String, AnyRef], version: Option[Long]): T = {
    MEvent(
      etype       = m.get(EVT_TYPE_ESFN)
        .map( JacksonParsing.stringParser )
        .flatMap(MEventTypes.withValueOpt)
        .get,
      ownerId     = m.get(OWNER_ID_ESFN)
        .map( JacksonParsing.stringParser )
        .get,
      argsInfo    = m.get(ARGS_ESFN)
        .fold [ArgsInfo] (EmptyArgsInfo) (ArgsInfo.fromJacksonJson),
      dateCreated = m.get(DATE_CREATED_ESFN)
        .fold(OffsetDateTime.now)(JacksonParsing.dateTimeParser),
      isCloseable = m.get(IS_CLOSEABLE_ESFN)
        .fold(isCloseableDflt)(JacksonParsing.booleanParser),
      isUnseen    = m.get(IS_UNSEEN_ESFN)
        .fold(false)(JacksonParsing.booleanParser),
      id          = id,
      versionOpt  = version
    )
  }

  /** Кешируем почти-собранный инстанс десериализатора экземпляров модели. */
  private val _reads0 = {
    (__ \ EVT_TYPE_ESFN).read[MEventType] and
    (__ \ OWNER_ID_ESFN).read[String] and
    (__ \ ARGS_ESFN).readNullable[ArgsInfo]
      .map(_.getOrElse(EmptyArgsInfo)) and
    (__ \ DATE_CREATED_ESFN).readNullable[OffsetDateTime]
      .map(_.getOrElse( OffsetDateTime.now )) and
    (__ \ IS_CLOSEABLE_ESFN).readNullable[Boolean]
      .map(_ getOrElse isCloseableDflt) and
    (__ \ IS_UNSEEN_ESFN).readNullable[Boolean]
      .map(_.getOrElseTrue)
  }

  /** Вернуть JSON reads для десериализации тела документа с имеющимися метаданными. */
  override protected def esDocReads(meta: IEsDocMeta): Reads[MEvent] = {
    _reads0 {
      (etype, ownerId, argsInfo, dateCreated, isCloseable, isUnseen) =>
        MEvent(etype, ownerId, argsInfo, dateCreated, isCloseable, isUnseen, id = meta.id, versionOpt = meta.version)
    }
  }


  override def writeJsonFields(m: T, acc: FieldsJsonAcc): FieldsJsonAcc = {
    import m._

    var acc: FieldsJsonAcc = List(
      EVT_TYPE_ESFN     -> JsString(etype.value),
      OWNER_ID_ESFN     -> JsString(ownerId),
      DATE_CREATED_ESFN -> JacksonParsing.date2JsStr(dateCreated)
    )
    if (isUnseen)
      acc ::= IS_UNSEEN_ESFN -> JsBoolean(isUnseen)
    if (argsInfo.nonEmpty)
      acc ::= ARGS_ESFN -> Json.toJson(argsInfo)
    if (isCloseable != isCloseableDflt)
      acc ::= IS_CLOSEABLE_ESFN -> JsBoolean(isCloseable)
    acc
  }


}

/** Интерфейс к полю MEvents, инстанс которого приходит через DI. */
trait IMEvents {
  def mEvents: MEvents
}


/** Класс-экземпляр одной нотификации. */
case class MEvent(
  etype         : MEventType,
  ownerId       : String,
  argsInfo      : ArgsInfo        = MEvent.argsDflt,
  dateCreated   : OffsetDateTime  = MEvent.dateCreatedDflt,
  isCloseable   : Boolean         = MEvent.isCloseableDflt,
  isUnseen      : Boolean         = true,
  ttlDays       : Option[Int]     = Some(MEvent.TTL_DAYS_UNSEEN),
  id            : Option[String]  = None,
  versionOpt    : Option[Long]    = None
)
  extends EsModelT
    with IMEvent


trait MEventsJmxMBean extends EsModelJMXMBeanI
final class MEventsJmx @Inject() (
                                   override val companion     : MEvents,
                                   override val esModelJmxDi  : EsModelJmxDi,
                                 )
  extends EsModelJMXBaseImpl
  with MEventsJmxMBean
{
  override type X = MEvent
}


/** Минимальный интерфейс абстрактного события.
  * Используется для рендера шаблонов, аргументы которых абстрагированы от конкретной реализации. */
trait IEvent extends OptStrId with Event {
  def etype         : MEventType
  def ownerId       : String
  def argsInfo      : ArgsInfo
  def dateCreated   : OffsetDateTime
  def isCloseable   : Boolean
  def isUnseen      : Boolean
}

/** Частичная реализация [[IEvent]] с реализацией getClassifier() в рамках текущей модели. */
trait IMEvent extends IEvent {
  override def getClassifier: Classifier = {
    MEvent.getClassifier(etype = Some(etype), ownerId = Some(ownerId), argsInfo = argsInfo)
  }
}


/**
 * Реализация [[IEvent]] для нехранимого события, т.е. когда что-то нужно отрендерить в режиме БЫСТРАБЛДЖАД!
 *
 * @param etype Тип события.
 * @param ownerId id owner'а.
 * @param argsInfo Необязательная инфа по параметрам [[EmptyArgsInfo]].
 * @param dateCreated Дата создания [now].
 * @param isCloseable Закрывабельность [false].
 * @param isUnseen Юзер в первый раз видит событие? [конечно true].
 */
case class MEventTmp(
  etype       : MEventType,
  ownerId     : String,
  argsInfo    : ArgsInfo        = EmptyArgsInfo,
  dateCreated : OffsetDateTime  = OffsetDateTime.now(),
  isCloseable : Boolean         = false,
  isUnseen    : Boolean         = true,
  id          : Option[String]  = None
) extends IMEvent


