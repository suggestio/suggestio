package io.suggest.model.inx2

import io.suggest.model._
import EsModel._
import org.elasticsearch.common.xcontent.XContentBuilder
import io.suggest.util.SioEsUtil._
import MInx._
import io.suggest.ym.model.MAd
import io.suggest.util.{MacroLogsImpl, SioEsUtil}
import com.fasterxml.jackson.annotation.JsonIgnore
import org.elasticsearch.action.deletebyquery.DeleteByQueryRequestBuilder
import scala.concurrent.ExecutionContext
import org.elasticsearch.client.Client
import io.suggest.event.SioNotifierStaticClientI
import play.api.libs.json.JsString

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 18.03.14 11:35
 * Description: Метаданные об индексах ТЦ представляются и хранятся в этой модели.
 */
object MMartInx extends EsModelStaticT with MacroLogsImpl {

  override val ES_TYPE_NAME: String = "inxMart"

  override type T = MMartInx

  override protected def dummy(martId: Option[String], version: Option[Long]) = {
    MMartInx(martId = martId.orNull, targetEsInxName = null)
  }


  override def generateMappingStaticFields: List[Field] = List(
    FieldSource(enabled = true),
    FieldAll(enabled = false)
  )

  override def generateMappingProps: List[DocField] = List(
    FieldString(MART_ID_ESFN, index = FieldIndexingVariants.not_analyzed, include_in_all = false),
    FieldString(ES_INX_NAME_ESFN, index = FieldIndexingVariants.not_analyzed, include_in_all = false)
  )


  def applyKeyValue(acc: MMartInx): PartialFunction[(String, AnyRef), Unit] = {
    case (MART_ID_ESFN, value)      => acc.martId = stringParser(value)
    case (ES_INX_NAME_ESFN, value)  => acc.targetEsInxName = stringParser(value)
  }

}

import MMartInx._

case class MMartInx(
  var martId: String,
  var targetEsInxName: String
) extends EsModelT with MSingleInxT {
  override type T = MMartInx

  @JsonIgnore def id: Option[String] = Some(martId)

  override def companion = MMartInx
  override def versionOpt = None

  def writeJsonFields(acc: FieldsJsonAcc): FieldsJsonAcc = {
    MART_ID_ESFN -> JsString(martId) ::
    ES_INX_NAME_ESFN -> JsString(targetEsInxName) ::
    acc
  }


  @JsonIgnore override def esTypePrefix: String = martId + "_"
  @JsonIgnore override val targetEsType = esTypePrefix + MAd.ES_TYPE_NAME


  override def esInxSettings(shards: Int, replicas: Int = 1): XContentBuilder = {
    SioEsUtil.getIndexSettingsV2(shards=shards, replicas=replicas)
  }

  /** Генерация MMartAd-маппингов. */
  def esAdMapping(esTypeSuffix: String): XContentBuilder = jsonGenerator { implicit b =>
    IndexMapping(
      typ = if (esTypeSuffix startsWith esTypePrefix) esTypeSuffix else esTypePrefix + esTypeSuffix,
      staticFields = MAd.generateMappingStaticFields,
      properties = MAd.generateMappingProps
    )
  }

  override def esInxMappings: Seq[(String, XContentBuilder)] = {
    esTypes map { esTypeName =>
      esTypeName -> esAdMapping(esTypeName)
    }
  }


  /** Выставить параметры для delete-by-query реквеста. Код тот же, но эта совместимость только на уровне исходников. */
  override def prepareDeleteByQueryRequest(req: DeleteByQueryRequestBuilder): DeleteByQueryRequestBuilder = {
    super.prepareDeleteByQueryRequest(req)
      .setIndices(targetEsInxName)
      .setTypes(targetEsType)
  }

}


/** JMX MBean интерфейс */
trait MMartInxJmxMBean extends EsModelJMXMBeanCommon

/** JMX MBean реализация. */
final class MMartInxJmx(implicit val ec: ExecutionContext, val client: Client, val sn: SioNotifierStaticClientI)
  extends EsModelJMXBase with MMartInxJmxMBean {
  def companion = MMartInx
}

