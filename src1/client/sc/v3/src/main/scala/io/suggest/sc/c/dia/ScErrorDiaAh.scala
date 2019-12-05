package io.suggest.sc.c.dia

import diode.{ActionHandler, ActionResult, ModelRW}
import io.suggest.sc.m.{CloseError, RetryError, SetErrorState}
import io.suggest.sc.m.dia.err.MScErrorDia
import io.suggest.sjs.common.async.AsyncUtil.defaultExecCtx

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 06.12.2019 0:23
  * Description: Контроллер диалога ошибки.
  */
class ScErrorDiaAh[M](
                       modelRW: ModelRW[M, Option[MScErrorDia]]
                     )
  extends ActionHandler(modelRW)
{

  override protected def handle: PartialFunction[Any, ActionResult[M]] = {

    case m: SetErrorState =>
      val v0 = value

      if (v0.fold(true)(_.pot.isPending)) {
        val v2 = Some(m.scErr)
        updated(v2)

      } else {
        // Две+ статические ошибки не выводим, просто оставляем всё как есть.
        noChange
      }


    // Запрошен повтор ошибочного экшена.
    case RetryError =>
      val v0 = value

      (for {
        errDia0 <- v0
        if !errDia0.pot.isPending
        retryAction <- errDia0.retryAction
      } yield {
        val fx = retryAction.toEffectPure
        val v2 = (MScErrorDia.pot modify (_.pending()))( errDia0 )
        updated(Some(v2), fx)
      })
        .getOrElse( noChange )


    // Закрыть диалог ошибки.
    case CloseError =>
      value.fold(noChange) { _ =>
        updated( None )
      }

  }

}
