package io.suggest.es.util

import org.elasticsearch.client.Client

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 14.10.15 10:33
 * Description: Интерфейс для di-поля с es-клиентом.
 */
trait IEsClient {

  /** Инстанс стандартного elasticsearch java client'а. */
  implicit def esClient: Client

}



