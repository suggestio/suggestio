package io.suggest.lk.adv.geo.a.geo.rad

import diode.{ActionHandler, ActionResult, Effect, ModelRW}
import io.suggest.geo.IGeoPointField
import io.suggest.lk.adv.geo.a._
import io.suggest.lk.adv.geo.m.MRad
import io.suggest.lk.adv.geo.u.LkAdvGeoFormUtil

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

  /** Действия работы с радиусом очень одинаковы как при drag, так и при drag end. */
  private def _handleNewRadiusXY(rd: IGeoPointField, stillDragging: Boolean) = {
    val v0 = value.get

    // Посчитать радиус:
    val rmGp2 = rd.geoPoint

    // Считаем расстояние между новым радиусом и исходным центром.
    val distanceM = LkAdvGeoFormUtil.distanceBetween(v0.circle.center, rmGp2)

    val v2 = v0.copy(
      circle  = v0.circle.withRadiusM( Math.abs(distanceM) ),
      state   = v0.state.copy(
        radiusDragging      = stillDragging,
        radiusMarkerCoords  = rmGp2
      )
    )

    updated( Some(v2) )
  }


  override protected def handle: PartialFunction[Any, ActionResult[M]] = {
    // Сначала частые действия в целях ускорения работы:

    // Пришла команда изменения центра круга в ходе таскания.
    case rcd: RadCenterDragging =>
      val v0 = value.get
      val v2 = v0.withState(
        v0.state.withCenterDragging( Some(rcd.geoPoint) )
      )
      updated( Some(v2) )

    // Происходит таскание маркера радиуса.
    case rd: RadiusDragging =>
      _handleNewRadiusXY(rd, stillDragging = true)

    // Теперь разовые действия, порядок обработки которых не важен:

    // Реакция на начало таскания центра круга.
    case RadCenterDragStart =>
      val v0 = value.get
      val v2 = v0.withState(
        v0.state.withCenterDragging( Some(v0.circle.center) )
      )
      updated( Some(v2) )

    // Реакция на окончание таскания центра круга.
    case rcde: RadCenterDragEnd =>
      val v0 = value.get

      val rmc0 = v0.state.radiusMarkerCoords
      val c0 = v0.circle.center
      val c2 = rcde.geoPoint

      val v2 = v0.copy(
        // Выставить новый центр круга в состояние.
        circle = v0.circle.withCenter( c2 ),
        state  = v0.state.copy(
          centerDragging = None,
          // Пересчитать координаты маркера радиуса.
          radiusMarkerCoords = rmc0.copy(
            // В прошлой версии были проблемы с дедубликацией этого кода: ошибочно прибавлялось +1.
            lat = rmc0.lat + c2.lat - c0.lat,
            lon = rmc0.lon + c2.lon - c0.lon
          )
        )
      )

      updated( Some(v2) )


    // Реакция на начало таскания маркера радиуса.
    case RadiusDragStart =>
      val v0 = value.get
      val v2 = v0.withState(
        v0.state.withRadiusDragging( true )
      )
      updated( Some(v2) )

    // Окончание таскания радиуса.
    case rde: RadiusDragEnd =>
      _handleNewRadiusXY(rde, stillDragging = false)

  }

}
