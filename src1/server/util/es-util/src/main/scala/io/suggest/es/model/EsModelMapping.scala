package io.suggest.es.model

import io.suggest.es.util.SioEsUtil._
import io.suggest.util._
import io.suggest.util.logs.IMacroLogs
import org.elasticsearch.common.xcontent.XContentBuilder

import scala.concurrent.Future

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 16.10.15 18:35
 * Description: Трейты для ES-моделей, связанные с маппингами типов.
 */


/** Интерфейс для статически чаастей ES-моделей для метода генерации описалова ES-полей. */
trait IGenEsMappingProps {
  /** Список ES-полей модели. */
  def generateMappingProps: List[DocField]
}

/** Дефолтовая реализация для [[IGenEsMappingProps]]. */
trait GenEsMappingPropsDummy extends IGenEsMappingProps {
  override def generateMappingProps: List[DocField] = Nil
}

/** Самые базовые функции генерации маппингов. */
trait EsModelStaticMappingGenerators extends IGenEsMappingProps {

  def generateMappingStaticFields: List[Field]
  def generateMappingProps: List[DocField]

  def generateMappingFor(typeName: String): XContentBuilder = {
    jsonGenerator { implicit b =>
      // Собираем маппинг индекса.
      IndexMapping(
        typ = typeName,
        staticFields = generateMappingStaticFields,
        properties = generateMappingProps
      )
    }
  }

}


/** Трейт содержит статические хелперы для работы с маппингами. */
trait EsModelStaticMapping extends EsModelStaticMappingGenerators with IMacroLogs with IEsModelDi {

  import mCommonDi._

  def ES_INDEX_NAME   = EsModelUtil.DFLT_INDEX
  def ES_TYPE_NAME: String
  def SHARDS_COUNT    = EsModelUtil.SHARDS_COUNT_DFLT
  def REPLICAS_COUNT  = EsModelUtil.REPLICAS_COUNT_DFLT

  def generateMapping: XContentBuilder = generateMappingFor(ES_TYPE_NAME)

  /** Отправить маппинг в elasticsearch. */
  def putMapping(): Future[Boolean] = {
    LOGGER.debug(s"putMapping(): $ES_INDEX_NAME/$ES_TYPE_NAME")
    esClient.admin().indices()
      .preparePutMapping(ES_INDEX_NAME)
      .setType(ES_TYPE_NAME)
      .setSource(generateMapping)
      .execute()
      .map { _.isAcknowledged }
  }

  def ensureIndex() = {
    EsModelUtil.ensureIndex(ES_INDEX_NAME, shards = SHARDS_COUNT, replicas = REPLICAS_COUNT)
  }
}

