package io.suggest.sc.c.search

import diode.{ActionHandler, ActionResult, Effect, ModelRW}
import io.suggest.geo.MGeoPoint
import io.suggest.maps.m.{IMapsAction, MapDragEnd, MapMoveEnd, OpenMapRcvr}
import io.suggest.maps.u.MapsUtil
import io.suggest.sc.m.inx.MapReIndex
import io.suggest.sc.m.search.{MMapDelay, MapDelayTimeOut}
import io.suggest.sjs.common.async.AsyncUtil.defaultExecCtx
import io.suggest.sjs.dom2.DomQuick
import japgolly.univeq._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 12.12.17 16:39
  * Description: Контроллер гео.карты для выдачи.
  * С гео.картами есть особенность, в частности с leaflet: они могут присылать по несколько
  * сигналов перемещения карты за один акт перемещения по карте.
  * Например, Leaflet присылает 2-3-4 сигнала: moveend, dragend, zoomend, которые могут дублироваться.
  *
  * Основное назначение контроллера: отсеивание паразитных событий.
  */
class ScMapDelayAh[M](
                       modelRW: ModelRW[M, Option[MMapDelay]]
                     )
  extends ActionHandler( modelRW )
{

  private def MAP_DRAG_END_TIMEOUT = 600

  private def RCVR_ID_CLICK_TIMEOUT = 250

  private def _mapMoveListen(reason: IMapsAction, geoPoint: Option[MGeoPoint]): ActionResult[M] = {
    val mri = MapReIndex( None, geoPoint, reason)
    _listenTimeout(mri, true, MAP_DRAG_END_TIMEOUT)
  }

  private def _listenTimeout(reason: MapReIndex, listenMove: Boolean, timeoutMs: Int): ActionResult[M] = {
    val gen = System.currentTimeMillis()
    val tp = DomQuick.timeoutPromiseT(timeoutMs)( MapDelayTimeOut(gen) )
    val v2 = MMapDelay(
      timerId     = tp.timerId,
      generation  = gen,
      reason      = reason,
      listenMove  = listenMove
    )
    val fx = Effect( tp.fut )
    updatedSilent( Some(v2), fx )
  }


  override protected def handle: PartialFunction[Any, ActionResult[M]] = {

    // Юзер таскает карту, значит надо среагировать на перемещение карты.
    case m: MapDragEnd =>
      _mapMoveListen(m, None)


    // Сигнал о перемещении карты.
    case m: MapMoveEnd =>
      (for {
        v0 <- value
        if v0.listenMove
      } yield {
        // Разрешено прослушивание map moveEnd событий. Перезапускаем таймер.
        DomQuick.clearTimeout( v0.timerId )

        val mgpOpt = Option( m.newCenterLL )
          .map( MapsUtil.latLng2geoPoint )

        // Выставить новый move-таймер
        _mapMoveListen(m, mgpOpt )
      })
        .getOrElse( noChange )


    // Клик по маркеру ресивера на карте. Отложить, чтобы карта не тормозила.
    case m: OpenMapRcvr =>
      for (v0 <- value)
        DomQuick.clearTimeout( v0.timerId )

      val reason = MapReIndex( Some(m.nodeId), None, m )
      _listenTimeout( reason, false, RCVR_ID_CLICK_TIMEOUT )


    // Срабатывание таймера.
    case m: MapDelayTimeOut =>
      (for {
        v0 <- value
        if v0.generation ==* m.gen
      } yield {
        val fx = v0.reason.toEffectPure
        updatedSilent( None, fx )
      })
        .getOrElse( noChange )


    /* // Нельзя тут трогать locationfound, т.к. он может происходить много раз вподряд,
       // TODO нужно только после L.control.locate.start() вызывать максимум один раз. Возможно, следует задействовать MapMoveEnd().
    case _: HandleLocationFound =>
      val fx = MapReIndex(
        rcvrId = None
      ).toEffectPure
      effectOnly(fx)
    */

  }

}
