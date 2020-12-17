package io.suggest.maps.c

import diode.{ActionHandler, ActionResult, Effect, ModelRW}
import io.suggest.adv.geo.AdvGeoConstants
import io.suggest.common.maps.rad.IMinMaxM
import io.suggest.geo.{CircleGs, IGeoPointField, MGeoPoint}
import io.suggest.maps.m._
import io.suggest.maps.u.MapsUtil


object RadAhUtil {

  def radCenterDragging[V <: MRadT[V]](v0: V, rcd: RadCenterDragging): V = {
    v0.withState(
      MRadS.centerDragging.set( Some(rcd.geoPoint) )(v0.state)
    )
  }

  def radCenterDragStart[V <: MRadT[V]](v0: V): V = {
    v0.withState(
      MRadS.centerDragging.set( Some(v0.circle.center) )(v0.state)
    )
  }

  def radCenterDragEnd[V <: MRadT[V]](v0: V, rcde: RadCenterDragEnd): V = {
    val rmc0 = v0.state.radiusMarkerCoords
    val c0 = v0.circle.center
    val c2 = rcde.geoPoint

    v0.withCircleState(
      // Выставить новый центр круга в состояние.
      circle = (CircleGs.center set c2)(v0.circle),
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
  }

  def radiusDragStart[V <: MRadT[V]](v0: V): V = {
    v0.withState(
      MRadS.radiusDragging.set(true)(v0.state)
    )
  }


  /** Статический метод для пересчёта абстрактного состояния rad.
    *
    * @param v0 Начальное состояние.
    * @param rmGp1 Новая координата маркера радиуса.
    * @param contraints Ограничения радиуса.
    * @param stillDragging Новое значение флага radiusDragging.
    * @tparam V Тип модели-реализации [[io.suggest.maps.m.MRadT]]
    * @return Новый инстанс модели-реализации [[io.suggest.maps.m.MRadT]].
    */
  def onRadiusDrag[V <: MRadT[V]](v0: V, contraints: IMinMaxM, rmGp1: MGeoPoint, stillDragging: Boolean): V = {

    // Считаем расстояние между новым радиусом и исходным центром.
    val distanceM = Math.abs(
      MapsUtil.distanceBetween(v0.circle.center, rmGp1)
    )

    // Принудительно запихиваем в границы.
    val radius2m = Math.max( contraints.MIN_M,
      Math.min( contraints.MAX_M, distanceM )
    )

    val circle2 = CircleGs.radiusM.set(radius2m)( v0.circle )

    // Не двигать радиус, вылезающий за пределы допустимых значений:
    val rmGp2 = if (radius2m != distanceM) {
      // TODO нужно подправлять координаты радиуса, чтобы учитывать угол на окружности.
      // Сейчас выехавший за пределы радиус оказывается на западе от центра независимо от угла.
      //v0.state.radiusMarkerCoords
      MapsUtil.radiusMarkerLatLng(circle2)
    } else {
      rmGp1
    }

    v0.withCircleState(
      circle  = circle2,
      state   = v0.state.copy(
        radiusDragging      = stillDragging,
        radiusMarkerCoords  = rmGp2
      )
    )

  }


}


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
    val v2 = RadAhUtil.onRadiusDrag(v0, AdvGeoConstants.Radius, rmGp1, stillDragging)
    Some(v2)
  }


  override protected def handle: PartialFunction[Any, ActionResult[M]] = {
    // Сначала частые действия в целях ускорения работы:

    // Пришла команда изменения центра круга в ходе таскания.
    case rcd: RadCenterDragging =>
      val v0 = value.get
      val v2 = RadAhUtil.radCenterDragging(v0, rcd)
      updated( Some(v2) )

    // Происходит таскание маркера радиуса.
    case rd: RadiusDragging =>
      val v2Opt = _handleNewRadiusXY(rd, stillDragging = true)
      updated(v2Opt)

    // Теперь разовые действия, порядок обработки которых не важен:

    // Реакция на начало таскания центра круга.
    case RadCenterDragStart =>
      val v0 = value.get
      val v2 = RadAhUtil.radCenterDragStart(v0)
      updated( Some(v2) )

    // Реакция на окончание таскания центра круга.
    case rcde: RadCenterDragEnd =>
      val v0 = value.get
      val v2 = RadAhUtil.radCenterDragEnd(v0, rcde)
      updated( Some(v2) )


    // Реакция на начало таскания маркера радиуса.
    case RadiusDragStart =>
      val v0 = value.get
      val v2 = RadAhUtil.radiusDragStart(v0)
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
        val mgc2 = CircleGs.center.set(center2)(mrad.circle)
        val radiusXy = MapsUtil.radiusMarkerLatLng(mgc2)
        val mrad2 = mrad.copy(
          circle = mgc2,
          state  = MRadS.radiusMarkerCoords.set(radiusXy)(mrad.state),
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
    case _: IRadClick =>
      _valueFold { mrad0 =>
        if (mrad0.centerPopup) {
          noChange
        } else {
          val mrad1 = mrad0.withCenterPopup( !mrad0.centerPopup )
          updated( Some(mrad1) )
        }
      }

    // Сигнал закрытия попапа.
    case HandleMapPopupClose if value.exists(_.centerPopup) =>
      val mrad0 = value.get
      val mrad1 = mrad0.withCenterPopup(false)
      updated( Some(mrad1) )

  }

  private def _valueFold(f: MRad => ActionResult[M]) = value.fold(noChange)(f)

}
