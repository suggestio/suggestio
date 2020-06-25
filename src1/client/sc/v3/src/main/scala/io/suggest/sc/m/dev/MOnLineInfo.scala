package io.suggest.sc.m.dev

import japgolly.univeq._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 25.06.2020 11:59
  * Description: Инфа по линку.
  */
object MOnLineInfo {

  @inline implicit def univEq: UnivEq[MOnLineInfo] = UnivEq.derive

}


/** Инфа, извлечённая из нативного инстанса NetworkInformation.
  *
  * @param hasLink Если ли коннекшен?
  * @param isFastEnought Достаточен ли канал для работы системы?
  */
case class MOnLineInfo(
                        hasLink           : Boolean,
                        isFastEnought     : Boolean,
                      )
