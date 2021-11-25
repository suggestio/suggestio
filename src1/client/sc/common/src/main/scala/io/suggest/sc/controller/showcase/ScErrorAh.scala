package io.suggest.sc.controller.showcase

import diode.{ActionHandler, ActionResult, ModelRW}
import io.suggest.log.Log
import io.suggest.msg.ErrorMsgs
import io.suggest.react.r.ComponentCatch
import io.suggest.sc.model.MScRoot

/** Controller for unexpected internal errors actions. */
final class ScErrorAh[M](
                          modelRW: ModelRW[M, MScRoot],
                        )
  extends ActionHandler( modelRW )
  with Log
{

  override protected def handle: PartialFunction[Any, ActionResult[M]] = {

    // Error from component.
    case m: ComponentCatch =>
      logger.error( ErrorMsgs.CATCHED_CONSTRUCTOR_EXCEPTION, msg = m )
      // TODO Render error info onto the screen.
      noChange

  }

}
