package io.suggest.sc.controller.dia

import diode.Implicits._
import io.suggest.sjs.common.async.AsyncUtil.defaultExecCtx
import diode.{ActionHandler, ActionResult, Effect, ModelRW}
import io.suggest.id.login.LoginFormCircuit
import io.suggest.lk.m.CsrfTokenEnsure
import io.suggest.sc.model.{ResetUrlRoute, ScLoginFormChange, ScLoginFormShowHide}
import io.suggest.sc.model.dia.MScLoginS
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

  private def _sc3PageModFx(nextLogin: Option[SioPages.Login]): Effect = {
    Effect.action {
      ResetUrlRoute(
        mods = Some { r =>
          (SioPages.Sc3.login replace nextLogin)(r())
        },
      )
    }
  }

  override protected def handle: PartialFunction[Any, ActionResult[M]] = {

    case m: ScLoginFormShowHide =>
      val v0 = value

      if (m.visible && v0.ident.isEmpty) {
        val fx = _sc3PageModFx( Some(SioPages.Login()) )
        val ensureCsrfFx = CsrfTokenEnsure().toEffectPure
        effectOnly( fx + ensureCsrfFx )

      } else if (!m.visible && v0.ident.nonEmpty) {
        val fx = _sc3PageModFx( None )
        effectOnly( fx )

      } else {
        noChange
      }


    case m: ScLoginFormChange =>
      val v0 = value

      m.loginPageOpt.fold {
        v0.ident.fold {
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
            val v2 = (MScLoginS.ident replace None)(v0)
            updated(v2)
          }
        }

      } { loginPage2 =>
        val loginFormCircuit = v0.ident getOrElse getLoginFormCircuit()

        val routeFx = Effect.action {
          loginFormCircuit.onRoute( loginPage2 )
          loginFormCircuit.showHideForm( true )
          DoNothing
        }

        v0.ident.fold {
          val v2 = (MScLoginS.ident replace Some(loginFormCircuit))(v0)
          updated(v2, routeFx)
        } { _ =>
          effectOnly( routeFx )
        }
      }

  }

}
