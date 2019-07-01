package io.suggest.id.login.c.reg

import diode.{ActionHandler, ActionResult, ModelRW}
import io.suggest.id.login.m.{SetPasswordBlur, SetPasswordEdit}
import io.suggest.id.login.m.reg.step4.MReg4SetPassword
import io.suggest.lk.m.input.MTextFieldS
import io.suggest.text.Validators
import japgolly.univeq._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 27.06.19 18:35
  * Description: Контроллер для страницы установки пароля для юзера.
  */
class Reg4SetPasswordAh[M](
                            modelRW: ModelRW[M, MReg4SetPassword],
                          )
  extends ActionHandler( modelRW )
{

  private def _pwLensFor( isRetype: Boolean ) = {
    if (isRetype) MReg4SetPassword.password2
    else MReg4SetPassword.password1
  }

  override protected def handle: PartialFunction[Any, ActionResult[M]] = {

    // Редактирование пароля в одном из полей ввода пароля.
    case m: SetPasswordEdit =>
      val v0 = value
      val pwLens = _pwLensFor( m.isRetype )
      val pw0 = pwLens.get(v0)

      if (pw0.value ==* m.value) {
        noChange

      } else {
        var updAccF = pwLens
          .composeLens( MTextFieldS.value )
          .set( m.value )

        // Сбросить не валидность нового пароля, если он валиден.
        if (!pw0.isValid && Validators.isPasswordValid(m.value)) {
          updAccF = updAccF andThen
            pwLens.composeLens( MTextFieldS.isValid ).set(true)
        }

        val v2 = updAccF( v0 )
        updated(v2)
      }


    case SetPasswordBlur =>
      val v0 = value

      // Провалидировать оба password-поля:
      (for {
        lens <- (MReg4SetPassword.password1 #:: MReg4SetPassword.password2 #:: Stream.empty)
        pw0 = lens.get(v0)
        isValid = Validators.isPasswordValid( pw0.value )
        if isValid !=* pw0.isValid
      } yield {
        lens
          .composeLens(MTextFieldS.isValid)
          .set(isValid)
      })
        .reduceOption( _ andThen _ )
        .fold(noChange) { updF =>
          val v2 = updF(v0)
          updated(v2)
        }

  }

}
