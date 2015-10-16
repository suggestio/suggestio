package io.suggest.model.es

import io.suggest.util.SioEsUtil._
import io.suggest.util._
import org.elasticsearch.client.Client
import org.elasticsearch.common.xcontent.XContentBuilder

import scala.concurrent.{ExecutionContext, Future}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 16.10.15 18:35
 * Description:
 */


trait IGenEsMappingProps {
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

  def generateMappingFor(typeName: String): XContentBuilder = jsonGenerator { implicit b =>
    // Собираем маппинг индекса.
    IndexMapping(
      typ = typeName,
      staticFields = generateMappingStaticFields,
      properties = generateMappingProps
    )
  }

}


/** Трейт содержит статические хелперы для работы с маппингами.
  * Однажды был вынесен из [[EsModelStaticMutAkvT]]. */
trait EsModelStaticMapping extends EsModelStaticMappingGenerators with MacroLogsI {

  def ES_INDEX_NAME   = EsModelUtil.DFLT_INDEX
  def ES_TYPE_NAME: String
  def SHARDS_COUNT    = EsModelUtil.SHARDS_COUNT_DFLT
  def REPLICAS_COUNT  = EsModelUtil.REPLICAS_COUNT_DFLT

  def generateMapping: XContentBuilder = generateMappingFor(ES_TYPE_NAME)

  /** Флаг, который можно перезаписать в реализации static-модели чтобы проигнорить конфликты при апдейте маппинга. */
  protected def mappingIgnoreConflicts: Boolean = false

  /** Отправить маппинг в elasticsearch. */
  def putMapping(ignoreConflicts: Boolean = mappingIgnoreConflicts)(implicit ec:ExecutionContext, client: Client): Future[Boolean] = {
    LOGGER.debug(s"putMapping(): $ES_INDEX_NAME/$ES_TYPE_NAME")
    client.admin().indices()
      .preparePutMapping(ES_INDEX_NAME)
      .setType(ES_TYPE_NAME)
      .setSource(generateMapping)
      .setIgnoreConflicts(ignoreConflicts)
      .execute()
      .map { _.isAcknowledged }
  }

  /** Удалить маппинг из elasticsearch. */
  def deleteMapping(implicit client: Client): Future[_] = {
    LOGGER.warn(s"deleteMapping(): $ES_INDEX_NAME/$ES_TYPE_NAME")
    client.admin().indices()
      .prepareDeleteMapping(ES_INDEX_NAME)
      .setType(ES_TYPE_NAME)
      .execute()
  }

  def ensureIndex(implicit ec:ExecutionContext, client: Client) = {
    EsModelUtil.ensureIndex(ES_INDEX_NAME, shards = SHARDS_COUNT, replicas = REPLICAS_COUNT)
  }
}

