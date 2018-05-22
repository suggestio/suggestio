package io.suggest.sc.ads

import io.suggest.dev.MScreen
import io.suggest.sc.MScApiVsn
import japgolly.univeq.UnivEq

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 22.05.18 23:10
  * Description: Общая модель аргументов реквеста карточек.
  */
object MScAdsArgs {

  implicit def univEq: UnivEq[MScAdsArgs] = UnivEq.derive



}

case class MScAdsArgs(
                       search    : MFindAdsReq,
                       foc       : Option[MScFocusArgs],
                       screen    : Option[MScreen],
                       apiVsn    : MScApiVsn
                     ) {

}
