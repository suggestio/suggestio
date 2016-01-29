package io.suggest.lk.adv.direct.m.price

import io.suggest.sjs.common.model.{TimestampedCompanion, Timestamped}

import scala.util.Try

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 29.01.16 12:29
 * Description: Контейнер для результата запроса цены к серверу.
 */
case class XhrResult(
  override val result     : Try[Resp],
  override val timestamp  : Long
)
  extends Timestamped[Resp]

object XhrResult extends TimestampedCompanion[Resp]

