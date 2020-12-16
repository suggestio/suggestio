package io.suggest.lk.adn.map.a

import diode.{ActionHandler, ActionResult, Effect, ModelRW}
import io.suggest.adn.mapf.AdnMapFormConstants
import io.suggest.geo.IGeoPointField
import io.suggest.lk.adn.map.m.MLamRad
import io.suggest.maps.c.RadAhUtil
import io.suggest.maps.m._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 20.04.17 15:04
  * Description: Action Handler для событий компонента управления на карте.
  *
  * Компонент бывает в нескольких режимах:
  * - отсутствовать, когда все галочки размещения убраны.
  * - rad, когда выставлена галочка размещения в геолокации.
  * - точка-маркер, если выставлена галочка размещения на карте рекламодателей,
  * но не выставлена галочка размещения в геолокации.
  *
  * Текущий режим задаются в M.opts, данные компонента в M.rad .
  */
class LamRadAh[M](
                      modelRW           : ModelRW[M, MLamRad],
                      priceUpdateFx     : Effect
                    )
  extends ActionHandler(modelRW)
{

  /** Действия работы с радиусом очень одинаковы как при drag, так и при drag end. */
  private def _handleNewRadiusXY(rd: IGeoPointField, stillDragging: Boolean): MLamRad = {
    val v0 = value
    // Посчитать радиус:
    val rmGp1 = rd.geoPoint
    RadAhUtil.onRadiusDrag(v0, AdnMapFormConstants.Rad.RadiusM, rmGp1, stillDragging)
  }


  override protected def handle: PartialFunction[Any, ActionResult[M]] = {

    // Пришла команда изменения центра круга в ходе таскания.
    case rcd: RadCenterDragging =>
      val v0 = value
      val v2 = RadAhUtil.radCenterDragging( v0, rcd )
      updated( v2 )

    // Происходит таскание маркера радиуса.
    case rd: RadiusDragging =>
      val v2Opt = _handleNewRadiusXY( rd, stillDragging = true )
      updated(v2Opt)

    // Теперь разовые действия, порядок обработки которых не важен:

    // Реакция на начало таскания центра круга.
    case RadCenterDragStart =>
      val v0 = value
      val v2 = RadAhUtil.radCenterDragStart( v0 )
      updated( v2 )

    // Реакция на окончание таскания центра круга.
    case rcde: RadCenterDragEnd =>
      val v0 = value
      val v2 = RadAhUtil.radCenterDragEnd( v0, rcde )
      updated( v2 )


    // Реакция на начало таскания маркера радиуса.
    case RadiusDragStart =>
      val v0 = value
      val v2 = RadAhUtil.radiusDragStart( v0 )
      updated( v2 )

    // Окончание таскания радиуса.
    case rde: RadiusDragEnd =>
      val v2Opt = _handleNewRadiusXY(rde, stillDragging = false)
      updated(v2Opt, priceUpdateFx)

  }

}
