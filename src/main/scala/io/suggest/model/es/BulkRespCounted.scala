package io.suggest.model.es

import org.elasticsearch.action.bulk.BulkResponse

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 24.09.15 17:53
 * Description: Модель для возврата результатов из bulk-методов, которые имеют внутренний счетчик результатов.
 */
case class BulkRespCounted(
  total: Long,
  bresp: BulkResponse
)
