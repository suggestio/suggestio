package models

import com.google.inject.{Singleton, Inject}
import io.suggest.model.es._
import util.PlayMacroLogsImpl
import EsModelUtil.FieldsJsonAcc
import play.api.libs.json._
import play.api.libs.functional.syntax._
import scala.collection.Map
import scala.concurrent.ExecutionContext
import io.suggest.event.SioNotifierStaticClientI
import org.elasticsearch.client.Client
import io.suggest.util.SioEsUtil._

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

  val NAME_ESFN = "name"
  val DATA_ESFN = "data"

  override def generateMappingStaticFields: List[Field] = List(
    FieldSource(enabled = true),
    FieldAll(enabled = true)
  )

  override def generateMappingProps: List[DocField] = List(
    FieldString(NAME_ESFN, index = FieldIndexingVariants.analyzed, include_in_all = true),
    FieldString(DATA_ESFN, index = FieldIndexingVariants.no, include_in_all = false)
  )

  @deprecated("Use deserializeOne2() instead", "2015.sep.05")
  override def deserializeOne(id: Option[String], m: Map[String, AnyRef], version: Option[Long]): T = {
    MCalendar(
      id          = id,
      name        = m.get(NAME_ESFN).fold("WTF?")(EsModelUtil.stringParser),
      companion   = this,
      data        = EsModelUtil.stringParser( m(DATA_ESFN) ),
      versionOpt  = version
    )
  }

  // Кеш для частично-собранного десериализатора.
  private val _reads0 = {
    (__ \ NAME_ESFN).read[String] and
    (__ \ DATA_ESFN).read[String]
  }

  override protected def esDocReads(meta: IEsDocMeta): Reads[T] = {
    _reads0 {
      (name, data) =>
        MCalendar(
          name        = name,
          data        = data,
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
  override val companion  : MCalendars,
  id                      : Option[String]  = None,
  versionOpt              : Option[Long]    = None
)
  extends EsModelPlayJsonT
  with EsModelT
{

  override type T = MCalendar

  override def writeJsonFields(acc: FieldsJsonAcc): FieldsJsonAcc = {
    companion.NAME_ESFN -> JsString(name) ::
      companion.DATA_ESFN -> JsString(data) ::
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
