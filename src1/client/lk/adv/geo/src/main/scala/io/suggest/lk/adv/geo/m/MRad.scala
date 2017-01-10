package io.suggest.lk.adv.geo.m

import diode.FastEq
import io.suggest.geo.MGeoCircle

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 10.01.17 13:25
  * Description: Модель-контейнер данных по компоненту настройки нового георазмещения.
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
                 circle      : MGeoCircle,
                 state       : MRadS
               ) {

  def withCircle(circle2: MGeoCircle) = copy(circle = circle2)
  def withState(state2: MRadS) = copy(state = state2)

}
