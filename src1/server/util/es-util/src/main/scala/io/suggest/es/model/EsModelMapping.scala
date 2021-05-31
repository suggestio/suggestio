package io.suggest.es.model

import io.suggest.es.MappingDsl

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 16.10.15 18:35
 * Description: Base trait for ES model static-part.
 */

trait EsModelStaticMapping {

  def ES_INDEX_NAME: String

  /** Deprecated in ES-6, removed in ES-7, but needed only for PUT Mapping API via ES java transport client. */
  def ES_TYPE_NAME: String

  /** Generate ES index mapping (internal DSL). */
  def indexMapping(implicit dsl: MappingDsl): dsl.IndexMapping

}

