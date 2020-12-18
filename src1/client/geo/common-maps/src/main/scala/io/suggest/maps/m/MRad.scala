package io.suggest.maps.m

import io.suggest.geo.{CircleGs, MGeoPoint}
import japgolly.univeq._
import monocle.macros.GenLens

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 10.01.17 13:25
  * Description: Модель-контейнер данных по компоненту настройки нового георазмещения.
  * Используется для окончательного компонента [[io.suggest.maps.r.rad.RadR]].
  */
object MRad {

  @inline implicit def univEq: UnivEq[MRad] = UnivEq.derive

  def circle = GenLens[MRad]( _.circle )
  def state = GenLens[MRad]( _.state )
  def enabled = GenLens[MRad]( _.enabled )

}


/**
  * Класс модели с кругом.
  * @param circle Состояние текущих параметров георазмещения в радиусе на карте.
  * @param state Состояние rad-компонентов.
  */
final case class MRad(
                       circle                   : CircleGs,
                       state                    : MRadS,
                       enabled                  : Boolean    = true,
                     ) {

  /** Вычислить текущий центр. */
  def currentCenter: MGeoPoint = {
    state.centerDragging
      .getOrElse( circle.center )
  }

}
