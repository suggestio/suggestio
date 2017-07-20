package io.suggest.sc.inx.c

import diode.{ActionHandler, ActionResult, ModelRW}
import io.suggest.sc.ScConstants
import io.suggest.sc.inx.m.{MWelcomeState, WcClick, WcTimeOut}
import io.suggest.sjs.common.async.AsyncUtil.defaultExecCtx

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 20.07.17 9:47
  * Description: Контроллер экрана приветствия.
  * Управляет только состоянием текущего отображения.
  */
class WelcomeAh[M](
                    modelRW: ModelRW[M, Option[MWelcomeState]]
                  )
  extends ActionHandler( modelRW )
{

  private def _nextPhase: ActionResult[M] = {
    value.fold {
      noChange
    } { v0 =>
      if (v0.isHiding) {
        updated(None)

      } else {
        // Сокрытие приветствия ещё не началось. Запустить таймер сокрытия приветствия.
        val fadeOutTstamp = System.currentTimeMillis()
        val tpF = WelcomeUtil.timeoutF(ScConstants.Welcome.FADEOUT_TRANSITION_MS * 0.5, fadeOutTstamp)

        val v2 = Some(
          v0.copy(
            isHiding    = true,
            timerTstamp = fadeOutTstamp
          )
        )

        updated(v2, tpF)
      }
    }
  }

  override protected def handle: PartialFunction[Any, ActionResult[M]] = {

    // Юзер долбит по приветствию, пытаясь ускорить процесс сокрытия приветствия.
    case WcClick =>
      _nextPhase

    case m: WcTimeOut =>
      if (value.exists(_.timerTstamp == m.timestamp))
        _nextPhase
      else
        noChange

  }

}
