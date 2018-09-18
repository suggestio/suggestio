package io.suggest.maps.m

import io.suggest.geo.CircleGs
import japgolly.univeq._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 10.01.17 13:25
  * Description: Модель-контейнер данных по компоненту настройки нового георазмещения.
  * Используется для окончательного компонента [[io.suggest.maps.r.rad.RadR]].
  */
object MRad {

  /** Поддержка FastEq для инстансов [[MRad]]. */
  implicit object MRadFastEq extends IMRadTFastEq[MRad] {
    override def eqv(a: MRad, b: MRad) = {
      super.eqv(a, b) &&
        (a.enabled ==* b.enabled) &&
        (a.centerPopup ==* b.centerPopup)
    }
  }

  @inline implicit def univEq: UnivEq[MRad] = UnivEq.derive

}


/**
  * Класс модели с кругом.
  * @param circle Состояние текущих параметров георазмещения в радиусе на карте.
  * @param state Состояние rad-компонентов.
  */
case class MRad(
                 override val circle      : CircleGs,
                 override val state       : MRadS,
                 enabled                  : Boolean    = true,
                 centerPopup              : Boolean    = false
               )
  extends MRadT[MRad]
{

  override def withCircle(circle2: CircleGs) = copy(circle = circle2)
  override def withState(state2: MRadS) = copy(state = state2)
  def withEnabled(enabled2: Boolean) = copy(enabled = enabled2)
  def withCenterPopup(enabled2: Boolean) = copy(centerPopup = enabled2)

}
