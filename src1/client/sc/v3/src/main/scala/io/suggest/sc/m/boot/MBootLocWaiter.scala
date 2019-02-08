package io.suggest.sc.m.boot

import diode.FastEq
import japgolly.univeq._
import io.suggest.ueq.UnivEqUtil._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 05.02.19 20:28
  * Description: Явно-пустое состояние ожидающего геолокацию компонента.
  */
object MBootLocWaiter {

  implicit object MBootLocWaiterFastEq extends FastEq[MBootLocWaiter] {
    override def eqv(a: MBootLocWaiter, b: MBootLocWaiter): Boolean = {
      (a.timerId ==* b.timerId)
    }
  }

  @inline implicit def univEq: UnivEq[MBootLocWaiter] = UnivEq.derive

}


/** Контейнер данных состояния waiter'а запуска геолокации.
  *
  * @param timerId id таймера для максимального таймаута.
  */
case class MBootLocWaiter(
                           timerId        : Int,
                         )
