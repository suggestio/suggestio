package io.suggest.sjs.common.tags.search

import io.suggest.sjs.common.model.{Timestamped, TimestampedCompanion}

import scala.util.Try

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 21.09.15 10:23
 * Description: Модель для заворачивания ответа [[MTagSearchResp]] в контейнер с данными о времени.
 */
case class MTagSearchRespTs(
  override val result     : Try[MTagSearchResp],
  override val timestamp  : Long
)
  extends Timestamped[MTagSearchResp]


object MTagSearchRespTs
  extends TimestampedCompanion[MTagSearchResp]
