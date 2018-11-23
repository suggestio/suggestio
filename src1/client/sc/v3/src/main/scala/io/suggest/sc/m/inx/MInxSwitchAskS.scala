package io.suggest.sc.m.inx

import diode.FastEq
import io.suggest.sc.index.MSc3IndexResp
import io.suggest.sc.m.HandleScApiResp
import japgolly.univeq.UnivEq
import io.suggest.ueq.UnivEqUtil._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 23.11.18 17:57
  * Description: Модель диалога подтверждения авто-переключения выдачи на новую локацию.
  */
object MInxSwitchAskS {

  implicit object MInxSwitchAskSFastEq extends FastEq[MInxSwitchAskS] {
    override def eqv(a: MInxSwitchAskS, b: MInxSwitchAskS): Boolean = {
      (a.okAction ===* b.okAction) &&
      (a.nextIndex ===* b.nextIndex)
    }
  }

  @inline implicit def univEq: UnivEq[MInxSwitchAskS] = UnivEq.derive

}


case class MInxSwitchAskS(
                           okAction     : HandleScApiResp,
                           nextIndex    : MSc3IndexResp,
                         )
