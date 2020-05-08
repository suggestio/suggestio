package io.suggest.spa

import diode.{ActionProcessor, ActionResult, Dispatcher}
import io.suggest.log.{ILogAction, Log}

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 08.11.2019 12:53
  */

object LoggingAllActionsProcessor extends Log {
  def apply[M <: AnyRef]: ActionProcessor[M] = {
    new ActionProcessor[M] {
      override def process(dispatch: Dispatcher, action: Any, next: Any => ActionResult[M], currentModel: M): ActionResult[M] = {
        if (!action.isInstanceOf[ILogAction])
          logger.log(msg = action)
        next(action)
      }
    }
  }
}
