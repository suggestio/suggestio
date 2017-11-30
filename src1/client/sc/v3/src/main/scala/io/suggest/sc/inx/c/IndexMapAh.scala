package io.suggest.sc.inx.c

import diode.{ActionHandler, ActionResult, Effect, ModelRW}
import io.suggest.maps.m.{MapDragEnd, ReqRcvrPopup}
import io.suggest.sc.inx.m.{GetIndex, MScIndex, MapDragEndDelayed, MapRcvrClickDelayed}
import io.suggest.sjs.common.async.AsyncUtil.defaultExecCtx
import io.suggest.sjs.common.controller.DomQuick

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 29.11.17 12:21
  * Description: Контроллер событий карты в контексте индекса выдачи,
  * т.е. подслушиваются экшены карты и принимается решение по индексу.
  */
class IndexMapAh[M](
                     modelRW   : ModelRW[M, MScIndex]
                   )
  extends ActionHandler( modelRW )
{

  private def _getIndexFx = {
    Effect.action {
      GetIndex(withWelcome = false)
    }
  }

  private def _getIndex(v2: MScIndex) = {
    updated(v2, _getIndexFx)
  }

  override protected val handle: PartialFunction[Any, ActionResult[M]] = {

    // Клик по ресиверу на карте. Перейти в выдачу выбранного узла, если он отличается от текущего.
    case m: ReqRcvrPopup =>
      // TODO Дожидаться MapMoveEnd-события.
      val fx = Effect {
        DomQuick
          .timeoutPromiseT(40)(MapRcvrClickDelayed(m))
          .fut
      }
      effectOnly(fx)

    case m: MapRcvrClickDelayed =>
      val v0 = value
      val nodeId = m.reason.nodeId
      if (v0.state.currRcvrId contains nodeId) {
        // Это текущий узел, ничего менять не требуется
        // TODO Показать попап на карте, чтобы человек понимал, что это текущий узел.
        noChange
      } else {
        // Переключаемся в новый узел.
        val v2 = v0.withState(
          v0.state.withRcvrNodeId( nodeId :: Nil )
        )
        _getIndex(v2)
      }


    // Окончено таскание карты.
    case _: MapDragEnd =>
      val fx = Effect {
        // TODO Выставить в состояние, что надо дождаться события MapMoveEnd, вместо этого таймаута.
        DomQuick
          .timeoutPromiseT(30)(MapDragEndDelayed)
          .fut
      }
      effectOnly( fx )

    // Окончено и ~завершено таскание карты.
    case MapDragEndDelayed =>
      val fx = _getIndexFx
      val v0 = value
      // Выкинуть из состояния текущего ресивера. Если местоположение особо и не изменилось, то сервер вернёт назад текущий ресивер.
      if (v0.state.currRcvrId.nonEmpty) {
        // Выход из узла на голую карту.
        val v2 = v0.withState(
          v0.state
            .withRcvrNodeId(Nil)
        )
        updated(v2, fx)

      } else {
        effectOnly(fx)
      }

  }

}
