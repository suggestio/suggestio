package io.suggest.model.inx2

import io.suggest.model._
import EsModel._
import io.suggest.ym.model.MMart.MartId_t
import org.elasticsearch.common.xcontent.XContentBuilder
import io.suggest.util.SioEsUtil._
import MInx._
import io.suggest.ym.model.MMartAd
import io.suggest.util.SioEsUtil
import com.fasterxml.jackson.annotation.JsonIgnore
import org.elasticsearch.action.search.SearchRequestBuilder
import org.elasticsearch.action.deletebyquery.DeleteByQueryRequestBuilder

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 18.03.14 11:35
 * Description: Метаданные об индексах ТЦ представляются и хранятся в этой модели.
 */
object MMartInx extends EsModelStaticT[MMartInx] {

  val ES_TYPE_NAME: String = "inxMart"

  protected def dummy(martId: String) = MMartInx(martId = martId, targetEsInxName = null)


  def generateMappingStaticFields: List[Field] = List(
    FieldSource(enabled = true),
    FieldAll(enabled = false)
  )

  def generateMappingProps: List[DocField] = List(
    FieldString(MART_ID_ESFN, index = FieldIndexingVariants.not_analyzed, include_in_all = false),
    FieldString(ES_INX_NAME_ESFN, index = FieldIndexingVariants.not_analyzed, include_in_all = false)
  )


  def applyKeyValue(acc: MMartInx): PartialFunction[(String, AnyRef), Unit] = {
    case (MART_ID_ESFN, value)      => acc.martId = martIdParser(value)
    case (ES_INX_NAME_ESFN, value)  => acc.targetEsInxName = stringParser(value)
  }

}

import MMartInx._

case class MMartInx(
  var martId: MartId_t,
  var targetEsInxName: String
) extends EsModelT[MMartInx] with MInxT {

  @JsonIgnore def esInxNames = Seq(targetEsInxName)

  @JsonIgnore def id: Option[String] = Some(martId)

  def companion = MMartInx

  def writeJsonFields(acc: XContentBuilder) {
    acc.field(MART_ID_ESFN, martId)
       .field(ES_INX_NAME_ESFN, targetEsInxName)
  }

  @JsonIgnore def esTypePrefix: String = martId + "_"
  @JsonIgnore val esType = esTypePrefix + MMartAd.ES_TYPE_NAME

  @JsonIgnore def esTypes = List(esType)

  def esInxSettings(shards: Int, replicas: Int = 1): XContentBuilder = {
    SioEsUtil.getIndexSettingsV2(shards=shards, replicas=replicas)
  }

  /** Генерация MMartAd-маппингов. */
  def esAdMapping(esTypeSuffix: String): XContentBuilder = jsonGenerator { implicit b =>
    IndexMapping(
      typ = if (esTypeSuffix startsWith esTypePrefix) esTypeSuffix else esTypePrefix + esTypeSuffix,
      staticFields = Seq(
        FieldSource(enabled = true),
        FieldAll(enabled = true)
      ),
      properties = MMartAd.generateMappingProps
    )
  }

  def esInxMappings: Seq[(String, XContentBuilder)] = {
    esTypes map { esTypeName =>
      esTypeName -> esAdMapping(esTypeName)
    }
  }


  /** Выставить параметры поискового реквеста. */
  override def prepareSearchRequest(req: SearchRequestBuilder): SearchRequestBuilder = {
    super.prepareSearchRequest(req)
      .setIndices(targetEsInxName)
      .setTypes(esType)
  }

  /** Выставить параметры для delete-by-query реквеста. Код тот же, но эта совместимость только на уровне исходников. */
  override def prepareDeleteByQueryRequest(req: DeleteByQueryRequestBuilder): DeleteByQueryRequestBuilder = {
    super.prepareDeleteByQueryRequest(req)
      .setIndices(targetEsInxName)
      .setTypes(esType)
  }

}

