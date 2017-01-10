package io.suggest.lk.adv.geo.m

import diode.FastEq
import io.suggest.adv.geo.MCircleS

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 10.01.17 13:25
  * Description:
  */
object MRad {

  /** Поддержка FastEq для инстансов [[MRad]]. */
  implicit object MRadFastEq extends FastEq[MRad] {
    override def eqv(a: MRad, b: MRad) = {
      (a.circle eq b.circle) &&
        (a.state eq b.state)
    }
  }

}

/**
  * Класс модели с кругом.
  * @param circle Состояние текущих параметров георазмещения в радиусе на карте.
  * @param state Состояние rad-компонентов.
  */
case class MRad(
  circle      : MCircleS,
  state       : MRadS
) {

  def withCircle(circle2: MCircleS) = copy(circle = circle2)
  def withState(state2: MRadS) = copy(state = state2)

}
