package io.suggest.spa

import diode.{ActionProcessor, ActionResult, Dispatcher}
import io.suggest.log.Log

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 08.11.2019 12:53
  * Description:
  */

object LoggingAllActionsProcessor extends Log {
  def apply[M <: AnyRef]: ActionProcessor[M] = {
    new ActionProcessor[M] {
      override def process(dispatch: Dispatcher, action: Any, next: Any => ActionResult[M], currentModel: M): ActionResult[M] = {
        logger.log(msg = action)
        next(action)
      }
    }
  }
}
