package io.suggest.sc.sjs.m.mfoc

import io.suggest.sc.sjs.m.msrv.foc.{MFocAdSearch, MScRespAdsFoc}
import io.suggest.sjs.common.model.{Timestamped, TimestampedCompanion}

import scala.util.Try

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 08.06.16 14:50
  * Description: Модель контейнера результата запроса.
  */

case class MFocSrvResp(
  resp    : MScRespAdsFoc,
  reqArgs : MFocAdSearch
)


/** Контейнер для try-результата с timestamp'ом внутрях. */
case class MFocSrvRespTs(
  override val result     : Try[MFocSrvResp],
  override val timestamp  : Long
)
  extends Timestamped[MFocSrvResp]
object MFocSrvRespTs
  extends TimestampedCompanion[MFocSrvResp]
