package io.suggest.sc.sjs.m.mfoc

import scala.concurrent.Future

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 08.06.16 15:18
  * Description: Модель данных состояния о текущем реквесте к серверу за focused-карточками.
  */
case class MFocReqInfo(
  timestamp: Long,
  fut      : Future[MFocSrvResp]
)
