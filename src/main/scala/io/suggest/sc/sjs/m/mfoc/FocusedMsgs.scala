package io.suggest.sc.sjs.m.mfoc

import io.suggest.sjs.common.fsm.{IFsmMsgCompanion, IFsmMsg}
import org.scalajs.dom.MouseEvent

// Команды для управления focused-выдачей.

/** Сигнал клика по кнопке скрытия focused-выдачи. */
case object CloseBtnClick extends IFsmMsg

/** Внутренний сигнал о завершении сокрытия focused-выдачи. */
case object FocRootDisappeared extends IFsmMsg

/** Сигнал по логотипу продьюсера focused-выдачи. */
case object ProducerLogoClick extends IFsmMsg

/** Внутренний сигнал о завершении появления на экране focused-выдачи. */
case object FocRootAppeared extends IFsmMsg

/** Клик мышкой по focused-выдаче рассматривается как листание влево-вправо. */
case class MouseClick(event: MouseEvent) extends IFsmMsg
object MouseClick extends IFsmMsgCompanion[MouseEvent]

// MouseMove обрабатывается через DirectDomEventHandler.
