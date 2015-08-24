package io.suggest.sc.sjs.m.mfsm.signals

import io.suggest.sc.sjs.m.mfsm.{IFsmMsgCompanion, IFsmMsg}
import org.scalajs.dom._


/** FSM-сигнал отжатия клавиши на клавиатуре. */
case class KbdKeyUp(e: KeyboardEvent)
  extends IFsmMsg


/** Сигнал об успешном завершении инициализации jsrouter'а. */
case object JsRouterReady
  extends IFsmMsg


/**
 * Сигнал о проблеме с инициализацией js router'а.
 * @param ex Исключение.
 */
case class JsRouterFailed(ex: Throwable)
  extends IFsmMsg
object JsRouterFailed
  extends IFsmMsgCompanion[Throwable]

