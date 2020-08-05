package io.suggest.sc.c.dia

import diode.Implicits._
import io.suggest.sjs.common.async.AsyncUtil.defaultExecCtx
import diode.{ActionHandler, ActionResult, Effect, ModelRW}
import io.suggest.id.login.LoginFormCircuit
import io.suggest.sc.m.{ResetUrlRoute, ScLoginFormChange, ScLoginFormShowHide}
import io.suggest.sc.m.dia.MScLoginS
import io.suggest.spa.{DoNothing, SioPages}

import scala.concurrent.duration._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 05.08.2020 21:00
  * Description: Управление состоянием формы логина.
  */
class ScLoginDiaAh[M](
                       getLoginFormCircuit  : () => LoginFormCircuit,
                       modelRW              : ModelRW[M, MScLoginS],
                     )
  extends ActionHandler(modelRW)
{

  private def _sc3PageModFx(nextLogin: Option[SioPages.Login]) = {
    Effect.action {
      ResetUrlRoute(
        mods = Some {
          SioPages.Sc3.login set nextLogin
        },
      )
    }
  }

  override protected def handle: PartialFunction[Any, ActionResult[M]] = {

    case m: ScLoginFormShowHide =>
      val v0 = value

      if (m.visible && v0.circuit.isEmpty) {
        val fx = _sc3PageModFx( Some(SioPages.Login()) )
        effectOnly(fx)

      } else if (!m.visible && v0.circuit.nonEmpty) {
        val fx = _sc3PageModFx( None )
        effectOnly(fx)

      } else {
        noChange
      }


    case m: ScLoginFormChange =>
      println(m)
      val v0 = value
      m.loginPageOpt.fold {
        v0.circuit.fold {
          noChange
        } { loginFormCircuit =>
          // Скрыть логин-форму. И потом удалить из состояния. В два шага, чтобы анимацию организовать.
          if (loginFormCircuit.isVisible()) {
            // Запуск анимированного сокрытия.
            val startHideFx = Effect.action {
              loginFormCircuit.showHideForm( false )
              DoNothing
            }
            val reDoFx = m.toEffectPure.after( 500.milliseconds )
            effectOnly( startHideFx >> reDoFx )

          } else {
            val v2 = (MScLoginS.circuit set None)(v0)
            updated(v2)
          }
        }

      } { loginPage2 =>
        val loginFormCircuit = v0.circuit getOrElse getLoginFormCircuit()

        val routeFx = Effect.action {
          loginFormCircuit.onRoute( loginPage2 )
          loginFormCircuit.showHideForm( true )
          DoNothing
        }

        v0.circuit.fold {
          val v2 = (MScLoginS.circuit set Some(loginFormCircuit))(v0)
          updated(v2, routeFx)
        } { _ =>
          effectOnly( routeFx )
        }
      }

  }

}
