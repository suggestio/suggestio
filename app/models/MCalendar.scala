package models

import io.suggest.model._
import util.PlayMacroLogsImpl
import io.suggest.model.EsModel.FieldsJsonAcc
import play.api.libs.json._
import play.api.libs.functional.syntax._
import scala.collection.Map
import scala.concurrent.ExecutionContext
import io.suggest.event.SioNotifierStaticClientI
import org.elasticsearch.client.Client

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 30.05.14 18:36
 * Description: Модель для хранения календарей в текстовых форматах.
 */
object MCalendar extends EsModelStaticT with PlayMacroLogsImpl with CurriedPlayJsonEsDocDeserializer {
  import io.suggest.util.SioEsUtil._
  import LOGGER._

  override type T = MCalendar

  override val ES_TYPE_NAME = "holyCal"

  val NAME_ESFN = "name"
  val DATA_ESFN = "data"

  // TODO Можно удалить, десериализация v2 тут заимплеменчена.
  override def deserializeOne(id: Option[String], m: Map[String, AnyRef], version: Option[Long]): T = {
    MCalendar(
      id = id,
      name = m.get(NAME_ESFN).fold("WTF?")(EsModel.stringParser),
      data = EsModel.stringParser( m(DATA_ESFN) ),
      versionOpt = version
    )
  }

  override def esDocReads: Reads[(Option[String], Option[Long]) => T] = (
    (__ \ NAME_ESFN).read[String] and
    (__ \ DATA_ESFN).read[String]
  ) {
    (name, data) => apply(name, data, _, _)
  }

  override def generateMappingStaticFields: List[Field] = List(
    FieldSource(enabled = true),
    FieldAll(enabled = true)
  )

  override def generateMappingProps: List[DocField] = List(
    FieldString(NAME_ESFN, index = FieldIndexingVariants.analyzed, include_in_all = true),
    FieldString(DATA_ESFN, index = FieldIndexingVariants.no, include_in_all = false)
  )
}


import MCalendar._


final case class MCalendar(
  name: String,
  data: String,
  id: Option[String] = None,
  versionOpt: Option[Long] = None
) extends EsModelPlayJsonT with EsModelT {

  override type T = MCalendar
  override def companion = MCalendar

  override def writeJsonFields(acc: FieldsJsonAcc): FieldsJsonAcc = {
    NAME_ESFN -> JsString(name)  ::  DATA_ESFN -> JsString(data)  ::  acc
  }
}

trait MCalendarJmxMBean extends EsModelJMXMBeanI
final class MCalendarJmx(implicit val ec: ExecutionContext, val client: Client, val sn: SioNotifierStaticClientI)
  extends EsModelJMXBase
  with MCalendarJmxMBean
{
  override def companion = MCalendar
}
