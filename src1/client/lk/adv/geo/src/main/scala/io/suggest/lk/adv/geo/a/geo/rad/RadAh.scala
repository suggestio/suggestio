package io.suggest.lk.adv.geo.a.geo.rad

import diode.{ActionHandler, ActionResult, Effect, ModelRW}
import io.suggest.lk.adv.geo.a.{RadCenterDragEnd, RadCenterDragStart, RadCenterDragging}
import io.suggest.lk.adv.geo.m.MRad

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 10.01.17 15:51
  * Description: Action handler для событий rad-компонента.
  */
class RadAh[M](
                modelRW           : ModelRW[M, Option[MRad]],
                priceUpdateFx     : Effect
              )
  extends ActionHandler(modelRW) {

  override protected def handle: PartialFunction[Any, ActionResult[M]] = {

    // Пришла команда изменения центра круга в ходе таскания.
    case rcd: RadCenterDragging =>
      val v0 = value.get
      val v2 = v0.withCircle(
        v0.circle.withCenter( rcd.geoPoint )
      )
      updated( Some(v2) )

    // Реакция на начало таскания центра круга.
    case RadCenterDragStart =>
      val v0 = value.get
      val v2 = v0.withState(
        v0.state.withCenterDragging(true)
      )
      updated( Some(v2) )

    // Реакция на окончание таскания центра круга.
    case rcde: RadCenterDragEnd =>
      val v0 = value.get
      val v2 = v0.copy(
        circle = v0.circle.withCenter( rcde.geoPoint ),
        state  = v0.state.withCenterDragging(false)
      )
      updated( Some(v2) )

  }

}
