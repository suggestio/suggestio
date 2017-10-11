package io.suggest.spa

import diode.{ActionType, Effect}
import io.suggest.sjs.common.async.AsyncUtil.defaultExecCtx
import japgolly.univeq.UnivEq

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 16.12.16 11:52
  * Description: Интерфейс для всех Diode-action'ов в целях достижения некоторой type-safety
  * для нетипизированных экшенов.
  * По смыслу этот интерфейс-маркер аналогичен IFsmMsg некоторых внутренних FSM.
  *
  * @see [[http://ochrons.github.io/diode/usage/Actions.html]]
  */
trait DAction {

  import DAction.DActionType

  final def effect = Effect.action( this )( DActionType, defaultExecCtx )

}


object DAction {

  implicit def univEq: UnivEq[DAction] = UnivEq.force

  /** Требуется ActionType[X] в scope, чтобы компилятор узрел экшен. */
  implicit object DActionType extends DActionType

}

trait DActionType extends ActionType[DAction]


/** Унифицированный для всех NOP-экшен, который не должен нигде отрабатываться. */
case object DoNothing extends DAction
