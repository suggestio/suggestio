package models

import io.suggest.model._
import util.PlayMacroLogsImpl
import io.suggest.model.EsModel.FieldsJsonAcc
import play.api.libs.json.JsString
import scala.concurrent.ExecutionContext
import io.suggest.event.SioNotifierStaticClientI
import org.elasticsearch.client.Client

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 30.05.14 18:36
 * Description: Модель для хранения календарей в текстовых форматах.
 */
object MCalendar extends EsModelStaticT with PlayMacroLogsImpl {
  import io.suggest.util.SioEsUtil._
  import LOGGER._

  override type T = MCalendar

  override val ES_TYPE_NAME = "holyCal"

  val NAME_ESFN = "name"
  val DATA_ESFN = "data"

  override protected def dummy(id: Option[String], version: Option[Long]): T = {
    MCalendar(
      id = id,
      versionOpt = version,
      name = null,
      data = null
    )
  }

  override def applyKeyValue(acc: T): PartialFunction[(String, AnyRef), Unit] = {
    case (NAME_ESFN, nameRaw) => acc.name = EsModel.stringParser(nameRaw)
    case (DATA_ESFN, dataRaw) => acc.data = EsModel.stringParser(dataRaw)
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


case class MCalendar(
  var name: String,
  var data: String,
  id: Option[String] = None,
  versionOpt: Option[Long] = None
) extends EsModelT {

  override type T = MCalendar
  override def companion = MCalendar

  override def writeJsonFields(acc: FieldsJsonAcc): FieldsJsonAcc = {
    NAME_ESFN -> JsString(name)  ::  DATA_ESFN -> JsString(data)  ::  acc
  }
}

trait MCalendarJmxMBean extends EsModelJMXMBeanCommon
class MCalendarJmx(implicit val ec: ExecutionContext, val client: Client, val sn: SioNotifierStaticClientI)
  extends EsModelJMXBase
  with MCalendarJmxMBean
{
  override def companion = MCalendar
}
