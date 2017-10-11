package io.suggest.spa.delay

import diode.FastEq
import japgolly.univeq._
import io.suggest.ueq.UnivEqUtil._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 11.10.17 17:17
  * Description: Состояние модуля откладывания экшенов на потом.
  */
object MDelayerS {

  def default = MDelayerS()

  implicit def univEq: UnivEq[MDelayerS] = UnivEq.derive

  implicit object MDelayerFastEq extends FastEq[MDelayerS] {
    override def eqv(a: MDelayerS, b: MDelayerS): Boolean = {
      (a.counter == b.counter) &&
        (a.delayed ===* b.delayed)
    }
  }

}


/** Класс модели состояния подсистемы отложенных экшенов.
  *
  * @param counter Счётчик для генерации уникальных значений.
  * @param delayed Мапа-каталог из отложенных экшенов.
  */
case class MDelayerS(
                      counter     : Int                        = 0,
                      delayed     : Map[Int, MDelayedAction]   = Map.empty
                    ) {

  def withDelayed( delayed: Map[Int, MDelayedAction] )    = copy(delayed = delayed)

}
