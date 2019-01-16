package io.suggest.es.model

import io.suggest.es.util.SioEsUtil.{jsonGenerator, DocField, Field, IndexMapping}
import org.elasticsearch.common.xcontent.XContentBuilder

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
trait EsModelStaticMapping extends EsModelStaticMappingGenerators {

  def ES_INDEX_NAME   = EsModelUtil.DFLT_INDEX
  def ES_TYPE_NAME: String
  def SHARDS_COUNT    = EsModelUtil.SHARDS_COUNT_DFLT
  def REPLICAS_COUNT  = EsModelUtil.REPLICAS_COUNT_DFLT

  def generateMapping: XContentBuilder = generateMappingFor(ES_TYPE_NAME)

}

