package io.suggest.sc.c.search

import diode.{ActionHandler, ActionResult, Effect, ModelRW}
import io.suggest.maps.m.{MapDragEnd, MapMoveEnd, OpenMapRcvr}
import io.suggest.sc.m.search.{MMapDelay, MapDelayTimeOut, MapReIndex}
import io.suggest.sjs.common.async.AsyncUtil.defaultExecCtx
import io.suggest.sjs.dom.DomQuick
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

  private def _mapMoveListen = _listenTimeout(None, true, MAP_DRAG_END_TIMEOUT)

  private def _listenTimeout(rcvrId: Option[String], listenMove: Boolean, timeoutMs: Int) = {
    val gen = System.currentTimeMillis()
    val tp = DomQuick.timeoutPromiseT(timeoutMs)( MapDelayTimeOut(gen) )
    val v2 = MMapDelay(
      timerId     = tp.timerId,
      generation  = gen,
      rcvrId      = rcvrId,
      listenMove  = listenMove
    )
    val fut = tp.fut
    val fx = Effect(fut)
    updatedSilent( Some(v2), fx )
  }


  override protected def handle: PartialFunction[Any, ActionResult[M]] = {

    // Юзер таскает карту, значит надо среагировать на перемещение карты.
    case _: MapDragEnd =>
      _mapMoveListen


    // Сигнал о перемещении карты.
    case _: MapMoveEnd =>
      val resOpt = for {
        v0 <- value
        if v0.listenMove
      } yield {
        // Разрешено прослушивание map moveEnd событий. Перезапускаем таймер.
        DomQuick.clearTimeout( v0.timerId )
        // Выставить новый move-таймер
        _mapMoveListen
      }
      resOpt
        .getOrElse( noChange )


    // Клик по маркеру ресивера на карте. Отложить, чтобы карта не тормозила.
    case m: OpenMapRcvr =>
      for (v0 <- value)
        DomQuick.clearTimeout( v0.timerId )

      _listenTimeout( Some(m.nodeId), false, RCVR_ID_CLICK_TIMEOUT )


    // Срабатывание таймера.
    case m: MapDelayTimeOut =>
      val resOpt = for {
        v0 <- value
        if v0.generation ==* m.gen
      } yield {
        val fx = MapReIndex(v0.rcvrId).toEffectPure
        updatedSilent( None, fx )
      }
      resOpt
        .getOrElse( noChange )


  }

}
