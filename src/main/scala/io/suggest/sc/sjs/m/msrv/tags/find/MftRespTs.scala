package io.suggest.sc.sjs.m.msrv.tags.find

import io.suggest.sjs.common.model.{Timestamped, TimestampedCompanion}

import scala.util.Try

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 21.09.15 10:23
 * Description: Модель для заворачивания ответа [[MftResp]] в контейнер с данными о времени.
 */
case class MftRespTs(
  override val result     : Try[MftResp],
  override val timestamp  : Long
)
  extends Timestamped[MftResp]


object MftRespTs
  extends TimestampedCompanion[MftResp]
