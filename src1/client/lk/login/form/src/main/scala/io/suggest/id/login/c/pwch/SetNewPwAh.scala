package io.suggest.id.login.c.pwch

import diode.{ActionHandler, ActionResult, ModelRW}
import io.suggest.id.login.m.pwch.MPwNew
import io.suggest.id.login.m.{NewPasswordBlur, SetPasswordEdit}
import io.suggest.lk.m.input.MTextFieldS
import io.suggest.text.Validators
import japgolly.univeq._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 27.06.19 18:35
  * Description: Контроллер для страницы установки пароля для юзера.
  */
class SetNewPwAh[M](
                     modelRW: ModelRW[M, MPwNew],
                   )
  extends ActionHandler( modelRW )
{

  private def _pwLensFor( isRetype: Boolean ) = {
    if (isRetype) MPwNew.password2
    else MPwNew.password1
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
          .andThen( MTextFieldS.value )
          .replace( m.value )

        // Сбросить не валидность нового пароля, если он валиден.
        if (!pw0.isValid && Validators.isPasswordValid(m.value)) {
          updAccF = updAccF andThen
            pwLens
              .andThen( MTextFieldS.isValid )
              .replace(true)
        }

        val v2 = updAccF( v0 )
        updated(v2)
      }


    case NewPasswordBlur =>
      val v0 = value

      // Если password mismatch, то подсветить несовпадение паролей.
      val updAccF0 = if (!v0.showPwMisMatch && !v0.isPasswordsMatch)
        (MPwNew.showPwMisMatch replace true) #:: LazyList.empty
      else
        LazyList.empty

      val validUpdFs = for {
        lens <- (MPwNew.password1 #:: MPwNew.password2 #:: LazyList.empty)
        pw0 = lens.get(v0)
        isValid = Validators.isPasswordValid( pw0.value )
        if isValid !=* pw0.isValid
      } yield {
        lens
          .andThen(MTextFieldS.isValid)
          .replace(isValid)
      }

      // Провалидировать оба password-поля:
      (validUpdFs #::: updAccF0)
        .reduceOption( _ andThen _ )
        .fold(noChange) { updF =>
          val v2 = updF(v0)
          updated(v2)
        }

  }

}
