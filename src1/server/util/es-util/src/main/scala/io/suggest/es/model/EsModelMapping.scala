package io.suggest.es.model

import io.suggest.es.MappingDsl
import io.suggest.es.util.SioEsUtil.{DocField, Field}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 16.10.15 18:35
 * Description: Трейты для ES-моделей, связанные с маппингами типов.
 */


/** Интерфейс для статически частей ES-моделей для метода генерации описалова ES-полей. */
trait IGenEsMappingProps {
  /** Список ES-полей модели. */
  def generateMappingProps: List[DocField]
}


/** Трейт содержит статические хелперы для работы с маппингами. */
trait EsModelStaticMapping extends IGenEsMappingProps {

  def ES_INDEX_NAME   = EsModelUtil.DFLT_INDEX
  def ES_TYPE_NAME: String
  def SHARDS_COUNT    = EsModelUtil.SHARDS_COUNT_DFLT
  def REPLICAS_COUNT  = EsModelUtil.REPLICAS_COUNT_DFLT

  def generateMappingStaticFields: List[Field]

  /** Сборка маппинга индекса по новому формату. */
  def indexMapping(implicit dsl: MappingDsl): dsl.IndexMapping

}

