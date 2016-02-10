package models.mcal

import com.google.inject.{Inject, Singleton}
import io.suggest.event.SioNotifierStaticClientI
import io.suggest.model.es.EsModelUtil.FieldsJsonAcc
import io.suggest.model.es._
import io.suggest.util.SioEsUtil._
import org.elasticsearch.client.Client
import play.api.libs.functional.syntax._
import util.PlayMacroLogsImpl
import play.api.libs.json._

import scala.collection.Map
import scala.concurrent.ExecutionContext

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 30.05.14 18:36
 * Description: Модель для хранения календарей в текстовых форматах.
 */
@Singleton
class MCalendars
  extends EsModelStaticT
  with PlayMacroLogsImpl
  with EsmV2Deserializer
{

  override type T = MCalendar

  override val ES_TYPE_NAME = "holyCal"

  /** Идентификаторы полей на стороне elasticsearch. */
  object Fields {
    val NAME_FN       = "name"
    val DATA_FN       = "data"
    /** Идентификатор локализации календаря среди множества оных. */
    val CAL_TYPE_FN   = "ctype"
  }

  override def generateMappingStaticFields: List[Field] = List(
    FieldSource(enabled = true),
    FieldAll(enabled = true)
  )


  import Fields._

  override def generateMappingProps: List[DocField] = List(
    FieldString(NAME_FN, index = FieldIndexingVariants.analyzed, include_in_all = true),
    FieldString(DATA_FN, index = FieldIndexingVariants.no, include_in_all = false),
    FieldString(CAL_TYPE_FN, index = FieldIndexingVariants.not_analyzed, include_in_all = false)
  )

  @deprecated("Use deserializeOne2() instead", "2015.sep.05")
  override def deserializeOne(id: Option[String], m: Map[String, AnyRef], version: Option[Long]): T = {
    MCalendar(
      id          = id,
      name        = m.get(NAME_FN).fold("WTF?")(EsModelUtil.stringParser),
      calType     = m.get(CAL_TYPE_FN)
        .map(EsModelUtil.stringParser)
        .flatMap(MCalTypes.maybeWithName)
        .getOrElse( _dfltCalType(id) ),
      companion   = this,
      data        = EsModelUtil.stringParser( m(DATA_FN) ),
      versionOpt  = version
    )
  }

  // Кеш для частично-собранного десериализатора.
  private val _dataReads0 = {
    (__ \ NAME_FN).read[String] and
    (__ \ DATA_FN).read[String] and
    (__ \ CAL_TYPE_FN).readNullable[MCalType]
  }

  /**
    * 2015.feb.10 появились идентификаторы локализации календарей.
    *
    * @param id id проблемного календаря.
    * @return Дефолтовый [[MCalType]].
    */
  private def _dfltCalType(id: Option[String]): MCalType = {
    val default = MCalTypes.default
    LOGGER.warn(s"No cal type defined for calendar[${id.orNull}], fallback to ${default.name}")
    default
  }

  override protected def esDocReads(meta: IEsDocMeta): Reads[T] = {
    _dataReads0 {
      (name, data, calTypeOpt) =>
        MCalendar(
          name        = name,
          data        = data,
          calType     = calTypeOpt.getOrElse(_dfltCalType(meta.id)),
          companion   = this,
          id          = meta.id,
          versionOpt  = meta.version
        )
    }
  }

}


case class MCalendar(
  name                    : String,
  data                    : String,
  calType                 : MCalType,
  override val companion  : MCalendars,
  id                      : Option[String]  = None,
  versionOpt              : Option[Long]    = None
)
  extends EsModelPlayJsonT
  with EsModelT
{

  override type T = MCalendar

  override def writeJsonFields(acc: FieldsJsonAcc): FieldsJsonAcc = {
    import companion.Fields._
    NAME_FN       -> JsString(name) ::
      DATA_FN     -> JsString(data) ::
      CAL_TYPE_FN -> JsString(calType.strId) ::
      acc
  }
}


// Поддержка JMX.
trait MCalendarJmxMBean extends EsModelJMXMBeanI

class MCalendarJmx @Inject() (
  override val companion  : MCalendars,
  override val ec         : ExecutionContext,
  override val client     : Client,
  override val sn         : SioNotifierStaticClientI
)
  extends EsModelJMXBase
  with MCalendarJmxMBean


/** Интерфейс для DI-поля MCalendars. */
trait IMCalendars {
  def mCalendars: MCalendars
}
