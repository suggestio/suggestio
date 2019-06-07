package io.suggest.id.login.c.reg

import diode.{ActionHandler, ActionResult, ModelRW}
import io.suggest.id.login.m._
import io.suggest.id.login.m.reg.step0.MReg0Creds
import io.suggest.lk.m.input.MTextFieldS
import io.suggest.sjs.common.log.Log
import io.suggest.text.Validators
import japgolly.univeq._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 26.03.19 12:15
  * Description: Контроллер регистрации по имени-паролю.
  */
class Reg0CredsAh[M](
                modelRW        : ModelRW[M, MReg0Creds],
              )
  extends ActionHandler( modelRW )
  with Log
{

  override protected def handle: PartialFunction[Any, ActionResult[M]] = {

    // Редактирования поля email.
    case m: RegEmailEdit =>
      val v0 = value
      if (v0.email.value ==* m.email) {
        noChange

      } else {
        var fieldUpdatesAccF = MTextFieldS.value.set(m.email)

        // Если isValid уже false, надо повторить валидацию.
        if (!v0.email.isValid && Validators.isEmailValid( m.email ))
          fieldUpdatesAccF = fieldUpdatesAccF andThen MTextFieldS.isValid.set( true )

        val v2 = MReg0Creds.email
          .modify( fieldUpdatesAccF )(v0)

        updated(v2)
      }


    // Расфокусировка поля email. Надо проверить валидность поля.
    case RegEmailBlur =>
      val v0 = value
      if (v0.email.isValid && !Validators.isEmailValid( v0.email.value )) {
        val v2 = MReg0Creds.email
          .composeLens( MTextFieldS.isValid )
          .set( false )(v0)
        updated( v2 )

      } else {
        noChange
      }


    // Редактирования номера телефона.
    case m: RegPhoneEdit =>
      val v0 = value
      if (v0.phone.value ==* m.phone) {
        noChange

      } else {
        var fieldUpdatesAccF = MTextFieldS.value.set( m.phone )
        // Если isValid уже false, то повторить валидацию.
        if ( !v0.phone.isValid && Validators.isPhoneValid(m.phone) )
          fieldUpdatesAccF = fieldUpdatesAccF andThen MTextFieldS.isValid.set( true )

        val v2 = MReg0Creds.phone
          .modify( fieldUpdatesAccF )(v0)
        updated(v2)
      }

    case RegPhoneBlur =>
      val v0 = value
      if (v0.phone.isValid && !Validators.isPhoneValid(v0.phone.value)) {
        val v2 = MReg0Creds.phone
          .composeLens( MTextFieldS.isValid )
          .set( false )(v0)
        updated(v2)
      } else {
        noChange
      }

  }

}
