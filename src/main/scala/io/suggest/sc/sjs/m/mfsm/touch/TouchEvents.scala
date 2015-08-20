package io.suggest.sc.sjs.m.mfsm.touch

import io.suggest.sc.sjs.m.mfsm.{IFsmMsgCompanion, IFsmMsg}
import org.scalajs.dom.TouchEvent

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 20.08.15 19:00
 * Description: Утиль для сборки touch-сигналов и сами touch-сигналы.
 */

/** Трейт классов touch-сигналов. */
trait ITouchEvent extends IFsmMsg {
  def event: TouchEvent
}
/** Трейт компаньонов touch-сигналов. */
trait ITouchEventCompanion extends IFsmMsgCompanion[TouchEvent]


/** Сигнал о начале касания. */
case class TouchStart(event: TouchEvent) extends ITouchEvent
object TouchStart extends ITouchEventCompanion


/** Сигнал о движении пальцев по экрану. */
case class TouchMove(event: TouchEvent) extends ITouchEvent
object TouchMove extends ITouchEventCompanion


/** Сигнал о завершении касания экрана. */
case class TouchEnd(event: TouchEvent) extends ITouchEvent
object TouchEnd extends ITouchEventCompanion


/** Сигнал о прерывании и отмене касания. */
case class TouchCancel(event: TouchEvent) extends ITouchEvent
object TouchCancel extends ITouchEventCompanion
