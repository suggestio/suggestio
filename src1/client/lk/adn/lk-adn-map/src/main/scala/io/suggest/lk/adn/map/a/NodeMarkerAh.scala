package io.suggest.lk.adn.map.a

import diode.{ActionHandler, ActionResult, ModelRW}
import io.suggest.lk.adn.map.m.MNodeMarkerS
import io.suggest.maps.m.{RadCenterDragEnd, RadCenterDragStart, RadCenterDragging}

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 20.04.17 15:04
  * Description:
  */
class NodeMarkerAh[M](
                       modelRW: ModelRW[M, MNodeMarkerS]
                     )
  extends ActionHandler(modelRW)
{

  override protected def handle: PartialFunction[Any, ActionResult[M]] = {

    // Пришла команда изменения центра круга в ходе таскания.
    case rcd: RadCenterDragging =>
      val v0 = value
      val v2 = v0.withDragging(
        Some(rcd.geoPoint)
      )
      updated( v2 )


    // Реакция на начало таскания центра круга.
    case RadCenterDragStart =>
      val v0 = value
      val v2 = v0.withDragging(
        Some(v0.center)
      )
      updated( v2 )

    // Реакция на окончание таскания центра круга.
    case rcde: RadCenterDragEnd =>
      val v0 = value
      val v2 = v0.copy(
        center   = rcde.geoPoint,
        dragging = None
      )
      updated( v2 )

  }

}
