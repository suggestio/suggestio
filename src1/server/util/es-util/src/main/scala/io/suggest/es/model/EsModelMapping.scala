package io.suggest.es.model

import io.suggest.es.MappingDsl

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 16.10.15 18:35
 * Description: Трейты для ES-моделей, связанные с маппингами типов.
 */

/** Трейт содержит статические хелперы для работы с маппингами. */
trait EsModelStaticMapping {

  def ES_INDEX_NAME: String
  def ES_TYPE_NAME: String

  /** Сборка маппинга индекса по новому формату. */
  def indexMapping(implicit dsl: MappingDsl): dsl.IndexMapping

}

