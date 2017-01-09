package io.suggest.sjs.mapbox.gl.event

import io.suggest.sjs.common.fsm.IFsmMsg

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 14.04.16 18:19
  * Description: Signals for FSMs.
  */

trait IMapSignal extends IFsmMsg {
  def eventData: EventData
}

/** Common interface for map signal object-companions. */
trait IMapSignalCompanion[T] {
  def apply(eventData: EventData): T
}


/** Интерфейс внутренних сигналов самому себе (MbFsm) о завершении инициализации карты. */
trait IMapInitDone extends IFsmMsg
/** Сигнал завершения инициализации карты. */
case class MapInitDone(override val eventData: EventData)
  extends IMapSignal
    with IMapInitDone
/** Этот object тоже можно использовать как сигнал, т.к. eventData обычно игнорится. */
object MapInitDone
  extends IMapSignalCompanion[MapInitDone]
  with IMapInitDone


trait IMapMoveSignal extends IMapSignal

/** Map dragging started. */
case class MoveStart(override val eventData: EventData)
  extends IMapMoveSignal
object MoveStart extends IMapSignalCompanion[MoveStart]


/** Map drag is still going on. */
case class Moving(override val eventData: EventData)
  extends IMapMoveSignal
object Moving extends IMapSignalCompanion[Moving]


/** Map drag finished. */
case class MoveEnd(override val eventData: EventData)
  extends IMapMoveSignal
object MoveEnd extends IMapSignalCompanion[MoveEnd]
