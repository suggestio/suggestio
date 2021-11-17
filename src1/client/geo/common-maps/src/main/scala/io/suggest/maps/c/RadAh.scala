package io.suggest.maps.c

import diode.{ActionHandler, ActionResult, Effect, ModelRW}
import io.suggest.common.maps.rad.IMinMaxM
import io.suggest.geo.{CircleGs, IGeoPointField, MGeoPoint}
import io.suggest.maps.m._
import io.suggest.maps.u.MapsUtil


object RadAhUtil {

  def radCenterDragging(v0: MRad, rcd: RadCenterDragging): MRad = {
    MRad.state
      .andThen( MRadS.centerDragging )
      .replace( Some(rcd.geoPoint) )(v0)
  }

  def radCenterDragStart(v0: MRad): MRad = {
    MRad.state
      .andThen( MRadS.centerDragging )
      .replace( Some(v0.circle.center) )(v0)
  }

  def radCenterDragEnd(v0: MRad, rcde: RadCenterDragEnd): MRad = {
    val rmc0 = v0.state.radiusMarkerCoords
    val c0 = v0.circle.center
    val c2 = rcde.geoPoint

    v0.copy(
      // Выставить новый центр круга в состояние.
      circle = (CircleGs.center replace c2)(v0.circle),
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

  def radiusDragStart(v0: MRad): MRad = {
    MRad.state
      .andThen( MRadS.radiusDragging )
      .replace( true )(v0)
  }


  /** Статический метод для пересчёта абстрактного состояния rad.
    *
    * @param v0 Начальное состояние.
    * @param rmGp1 Новая координата маркера радиуса.
    * @param contraints Ограничения радиуса.
    * @param stillDragging Новое значение флага radiusDragging.
    * @return Обновлённый MRad.
    */
  def onRadiusDrag(v0: MRad, contraints: IMinMaxM, rmGp1: MGeoPoint, stillDragging: Boolean): MRad = {
    // Считаем расстояние между новым радиусом и исходным центром.
    val distanceM = Math.abs(
      MapsUtil.distanceBetween(v0.circle.center, rmGp1)
    )

    // Принудительно запихиваем в границы.
    val radius2m = Math.max( contraints.MIN_M,
      Math.min( contraints.MAX_M, distanceM )
    )

    val circle2 = (CircleGs.radiusM replace radius2m)( v0.circle )

    // Не двигать радиус, вылезающий за пределы допустимых значений:
    val rmGp2 = if (radius2m != distanceM) {
      // TODO нужно подправлять координаты радиуса, чтобы учитывать угол на окружности.
      // Сейчас выехавший за пределы радиус оказывается на западе от центра независимо от угла.
      //v0.state.radiusMarkerCoords
      MapsUtil.radiusMarkerLatLng(circle2)
    } else {
      rmGp1
    }

    v0.copy(
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
                priceUpdateFx     : Effect,
                radiusMinMax      : IMinMaxM,
              )
  extends ActionHandler(modelRW)
{

  /** Действия работы с радиусом очень одинаковы как при drag, так и при drag end. */
  private def _handleNewRadiusXY(rd: IGeoPointField, stillDragging: Boolean): Option[MRad] = {
    val v0 = value.get
    // Посчитать радиус:
    val rmGp1 = rd.geoPoint
    val v2 = RadAhUtil.onRadiusDrag(v0, radiusMinMax, rmGp1, stillDragging)
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
        val mgc2 = (CircleGs.center replace center2)(mrad.circle)
        val radiusXy = MapsUtil.radiusMarkerLatLng(mgc2)
        val mrad2 = mrad.copy(
          circle = mgc2,
          state  = (MRadS.radiusMarkerCoords replace radiusXy)(mrad.state),
        )
        updated( Some(mrad2) )
      }

    // Сигнал включения/выключения rad-подсистемы.
    case RadOnOff(isEnabled) =>
      _valueFold { mrad0 =>
        val mrad1 = (MRad.enabled replace isEnabled)(mrad0)
        updated( Some(mrad1), priceUpdateFx )
      }

  }

  private def _valueFold(f: MRad => ActionResult[M]) = value.fold(noChange)(f)

}


/** Контроллер попапа над кругом размещения. */
final class RadPopupAh[M](
                           modelRW: ModelRW[M, Boolean],
                         )
  extends ActionHandler( modelRW )
{

  override protected def handle: PartialFunction[Any, ActionResult[M]] = {

    // Сигнал клика по некоторым rad-элементам.
    case _: IRadClick =>
      val v0 = value
      if (v0) {
        noChange
      } else {
        updated( true )
      }

    // Сигнал закрытия попапа.
    case HandleMapPopupClose =>
      if (!value) {
        noChange
      } else {
        updated( false )
      }

  }

}
