package io.suggest.sc.inx.c

import diode.{ActionHandler, ActionResult, Effect, ModelRW}
import io.suggest.maps.m.{MapDragEnd, MapDragStart, ReqRcvrPopup}
import io.suggest.sc.inx.m.{GetIndex, MScIndex}
import io.suggest.sjs.common.async.AsyncUtil.defaultExecCtx

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
      val v0 = value
      if (v0.state.currRcvrId contains m.nodeId) {
        // Это текущий узел, ничего менять не требуется
        // TODO Показать попап на карте, чтобы человек понимал, что это текущий узел.
        noChange
      } else {
        // Переключаемся в новый узел.
        val v2 = v0.withState(
          v0.state.withRcvrNodeId( m.nodeId :: Nil )
        )
        _getIndex(v2)
      }


    // Началось ручное перетаскивание карты.
    case MapDragStart =>
      // Выкинуть из состояния текущего ресивера, чтобы перейти в режим поиска по карте.
      val v0 = value
      if (v0.state.currRcvrId.nonEmpty) {
        // Выход из узла на голую карту.
        // TODO Не пашет, надо запускать GetIndex как-то совсем после сохранени состояния.
        val v2 = v0.withState(
          v0.state
            .withRcvrNodeId(Nil)
        )
        _getIndex(v2)

      } else {
        // Последовательные шаги по карте
        noChange
      }

    case m: MapDragEnd =>
      // TODO Проверять, изменилась ли начальная координата, и только тогда запускать эффект GetIndex'а.
      effectOnly( _getIndexFx )

  }

}
