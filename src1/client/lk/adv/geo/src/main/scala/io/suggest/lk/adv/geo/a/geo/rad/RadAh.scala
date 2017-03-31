package io.suggest.lk.adv.geo.a.geo.rad

import diode.{ActionHandler, ActionResult, Effect, ModelRW}
import io.suggest.adv.geo.AdvGeoConstants.Rad
import io.suggest.geo.IGeoPointField
import io.suggest.lk.adv.geo.a._
import io.suggest.lk.adv.geo.m._
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
  private def _handleNewRadiusXY(rd: IGeoPointField, stillDragging: Boolean): Option[MRad] = {
    val v0 = value.get

    // Посчитать радиус:
    val rmGp1 = rd.geoPoint

    // Считаем расстояние между новым радиусом и исходным центром.
    val distanceM = Math.abs(
      LkAdvGeoFormUtil.distanceBetween(v0.circle.center, rmGp1)
    )

    // Принудительно запихиваем в границы.
    val radius2m = Math.max( Rad.RADIUS_MIN_M,
      Math.min( Rad.RADIUS_MAX_M, distanceM )
    )

    val circle2 = v0.circle.withRadiusM( radius2m )

    // Не двигать радиус, вылезающий за пределы допустимых значений:
    val rmGp2 = if (radius2m != distanceM) {
      // TODO нужно подправлять координаты радиуса, чтобы учитывать угол на окружности.
      // Сейчас выехавший за пределы радиус оказывается на западе от центра независимо от угла.
      //v0.state.radiusMarkerCoords
      LkAdvGeoFormUtil.radiusMarkerLatLng(circle2)
    } else {
      rmGp1
    }

    val v2 = v0.copy(
      circle  = circle2,
      state   = v0.state.copy(
        radiusDragging      = stillDragging,
        radiusMarkerCoords  = rmGp2
      )
    )

    Some(v2)
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
      val v2Opt = _handleNewRadiusXY(rd, stillDragging = true)
      updated(v2Opt)

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
      val v2Opt = _handleNewRadiusXY(rde, stillDragging = false)
      updated(v2Opt, priceUpdateFx)


    // Найдена геолокация юзера. Переместить круг в новую точку, даже если происходит перетаскивание.
    // Карта ведь всё равно переедет на геолокацию.
    case hlf: HandleLocationFound =>
      _valueFold { mrad =>
        val center2 = hlf.geoPoint
        val mgc2 = mrad.circle.withCenter(center2)
        val radiusXy = LkAdvGeoFormUtil.radiusMarkerLatLng(mgc2)
        val mrad2 = mrad.copy(
          circle = mgc2,
          state  = mrad.state.withRadiusMarkerCoords(radiusXy)
        )
        updated( Some(mrad2) )
      }

    // Сигнал включения/выключения rad-подсистемы.
    case RadOnOff(isEnabled) =>
      _valueFold { mrad0 =>
        val mrad1 = mrad0.withEnabled(isEnabled)
        updated( Some(mrad1), priceUpdateFx )
      }

    // Сигнал клика по некоторым rad-элементам.
    case RadClick =>
      _valueFold { mrad0 =>
        if (mrad0.centerPopup) {
          noChange
        } else {
          val mrad1 = mrad0.withCenterPopup( !mrad0.centerPopup )
          updated( Some(mrad1) )
        }
      }

    // Сигнал закрытия попапа.
    case HandlePopupClose if value.exists(_.centerPopup) =>
      val mrad0 = value.get
      val mrad1 = mrad0.withCenterPopup(false)
      updated( Some(mrad1) )

  }

  private def _valueFold(f: MRad => ActionResult[M]) = value.fold(noChange)(f)

}
