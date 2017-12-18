package io.suggest.jd

import io.suggest.ueq.UnivEqUtil._
import diode.FastEq
import japgolly.univeq._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 18.12.17 14:14
  * Description:
  */
object MJdConfJs {

  /** Поддержка FastEq для инстансов [[MJdConf]]. */
  implicit object MJdConfFastEq extends FastEq[MJdConf] {
    override def eqv(a: MJdConf, b: MJdConf): Boolean = {
      (a.isEdit                ==* b.isEdit) &&
        (a.szMult             ===* b.szMult) &&
        (a.blockPadding       ===* b.blockPadding) &&
        (a.gridColumnsCount    ==* b.gridColumnsCount)
    }
  }

}
