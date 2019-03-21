package io.suggest.spa

import diode._
import japgolly.univeq.UnivEq

import scala.concurrent.ExecutionContext

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
trait DAction


object DAction {

  @inline implicit def univEq[T <: DAction]: UnivEq[T] = UnivEq.force

  /** Требуется ActionType[X] в scope, чтобы компилятор узрел экшен. */
  implicit object DActionType extends DActionType

  /** Дополнительные методы для экшенов. */
  implicit class DActionOpsExt(val act: DAction) extends AnyVal {

    /** Ускоренное заворачивание в Effect.action() с отбрасыванием ленивости исполнения.
      * Когда нет сайд-эффектов, нет тяжелых действия или сложных манипуляций, этого достаточно.
      */
    def toEffectPure(implicit ec: ExecutionContext): Effect =
      Effect.action(act)

  }

}

trait DActionType extends ActionType[DAction]


/** Унифицированный для всех NOP-экшен, который не должен нигде отрабатываться. */
case object DoNothing extends DAction


object DoNothingActionProcessor {

  /** Сборка ActionProcessor'а, который перехватывает заведомо пустые экшены. */
  def apply[M <: AnyRef]: ActionProcessor[M] = {
    new ActionProcessor[M] {
      override def process(dispatch: Dispatcher, action: Any, next: Any => ActionResult[M], currentModel: M): ActionResult[M] = {
        action match {
          case DoNothing | null =>
            ActionResult.NoChange
          case _ =>
            next( action )
        }
      }
    }
  }

}
