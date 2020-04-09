package io.suggest.sc.c.inx

import diode.{ActionHandler, ActionResult, ModelRW}
import io.suggest.sc.ScConstants
import io.suggest.sc.m.inx.{MWelcomeState, WcClick, WcTimeOut}
import io.suggest.sjs.common.async.AsyncUtil.defaultExecCtx
import io.suggest.sjs.dom2.DomQuick

import scala.concurrent.Future

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 20.07.17 9:47
  * Description: Контроллер экрана приветствия.
  * Управляет только состоянием текущего отображения.
  */
object WelcomeAh {

  /** Запуск таймера переключения фазы приветствия.
    *
    * @param afterMs Через сколько мс переключение?
    * @param tstamp Timestamp-маркер.
    * @return Возвращает фьючерс, исполняющийся через afterMs миллисекунд.
    */
  def timeout(afterMs: Double, tstamp: Long): Future[WcTimeOut] = {
    val tp = DomQuick.timeoutPromiseT( afterMs ) {
      WcTimeOut( tstamp )
    }
    tp.fut
  }

}

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
        val tpF = { () =>
          WelcomeAh.timeout(ScConstants.Welcome.FADEOUT_TRANSITION_MS * 0.5, fadeOutTstamp)
        }

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
