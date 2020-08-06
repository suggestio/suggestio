package io.suggest.proto.http.model

import japgolly.univeq._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 06.08.2020 9:42
  * Description: Модель необязательного конфига для запросов http-клиентов.
  */
object HttpClientConfig {

  def empty = apply()

  @inline implicit def univEq: UnivEq[HttpClientConfig] = UnivEq.derive

}


case class HttpClientConfig(
                             csrfToken      : Option[MCsrfToken]        = None,
                           )
