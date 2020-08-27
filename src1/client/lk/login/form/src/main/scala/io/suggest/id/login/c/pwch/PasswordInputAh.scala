package io.suggest.id.login.c.pwch

import diode.{ActionHandler, ActionResult, ModelRW}
import io.suggest.id.login.m.{PasswordBlur, SetPassword}
import io.suggest.lk.m.input.MTextFieldS
import io.suggest.text.Validators
import japgolly.univeq._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 10.07.19 15:25
  * Description:
  */
class PasswordInputAh[M](
                          modelRW: ModelRW[M, MTextFieldS],
                        )
  extends ActionHandler( modelRW )
{

  override protected def handle: PartialFunction[Any, ActionResult[M]] = {

    // Редактирование поля пароля.
    case m: SetPassword =>
      val v0 = value
      if (v0.value ==* m.password) {
        noChange

      } else {
        var updAccF = MTextFieldS.value.set( m.password )

        if (!v0.isValid && Validators.isPasswordValid(m.password))
          updAccF = updAccF andThen MTextFieldS.isValid.set(true)

        val v2 = updAccF(v0)

        updated(v2)
      }


    case PasswordBlur =>
      val v0 = value

      val isPwValid2 = Validators.isPasswordValid(v0.value)
      if (v0.isValid ==* isPwValid2) {
        noChange
      } else {
        val v2 = MTextFieldS.isValid.set( isPwValid2 )(v0)
        updated(v2)
      }

  }

}
