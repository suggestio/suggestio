package io.suggest.n2.edge.edit.c

import diode.{ActionHandler, ActionResult, ModelRW}
import io.suggest.lk.m.{ErrorPopupCloseClick, MErrorPopupS}

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 27.01.2020 16:48
  * Description: Контроллер для диалога вывода ошибок.
  */
class ErrorDiaAh[M](
                     modelRW      : ModelRW[M, Option[MErrorPopupS]],
                   )
  extends ActionHandler( modelRW )
{

  override protected def handle: PartialFunction[Any, ActionResult[M]] = {

    case ErrorPopupCloseClick =>
      val v0 = value
      v0.fold(noChange) { _ =>
        updated( None )
      }

  }

}
