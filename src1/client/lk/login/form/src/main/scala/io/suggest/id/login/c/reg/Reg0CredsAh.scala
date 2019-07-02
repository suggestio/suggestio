package io.suggest.id.login.c.reg

import diode.data.Pot
import diode.{ActionHandler, ActionResult, ModelRW}
import io.suggest.id.login.m._
import io.suggest.id.login.m.reg.MRegS
import io.suggest.id.login.m.reg.step0.MReg0Creds
import io.suggest.id.login.m.reg.step1.MReg1Captcha
import io.suggest.id.login.m.reg.step2.MReg2SmsCode
import io.suggest.lk.m.input.MTextFieldS
import io.suggest.sjs.common.log.Log
import io.suggest.text.Validators
import japgolly.univeq._
import monocle.PLens

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 26.03.19 12:15
  * Description: Контроллер регистрации по имени-паролю.
  */
class Reg0CredsAh[M](
                      modelRW        : ModelRW[M, MRegS],
                    )
  extends ActionHandler( modelRW )
  with Log
{


  private def _edit(plens: PLens[MRegS,MRegS, MTextFieldS,MTextFieldS], valueEdited: String,
                    isValid: String => Boolean): ActionResult[M] = {
    val v0 = value
    val p0 = plens.get(v0)
    if (p0.value ==* valueEdited) {
      noChange

    } else {
      var fieldUpdatesAccF = MTextFieldS.value.set( valueEdited )

      // Если isValid уже false, то повторить валидацию.
      if ( !p0.isValid && isValid(valueEdited) )
        fieldUpdatesAccF = fieldUpdatesAccF andThen MTextFieldS.isValid.set( true )

      var vUpdF = plens.modify( fieldUpdatesAccF )

      // Если уже готовы следующие шаги, то надо обнулить.
      if (v0.s0Creds.submitReq.nonEmpty) {
        vUpdF = vUpdF andThen (MRegS.s0Creds composeLens MReg0Creds.submitReq set Pot.empty)
        if (v0.s1Captcha.nonEmpty) {
          vUpdF = vUpdF andThen MRegS.s1Captcha.set( MReg1Captcha.empty )
          // Сбросить состояние смс-кода, если оно уже запрашивалось ранее.
          if (v0.s2SmsCode.nonEmpty)
            vUpdF = vUpdF andThen MRegS.s2SmsCode.set( MReg2SmsCode.empty )
        }
      }

      val v2 = vUpdF(v0)
      updated(v2)
    }
  }


  private def _blur(plens: PLens[MRegS,MRegS, MTextFieldS,MTextFieldS],
                    isValid: String => Boolean): ActionResult[M] = {
    val v0 = value
    val p0 = plens.get(v0)
    if (p0.isValid && !isValid( p0.value )) {
      val v2 = plens
        .composeLens( MTextFieldS.isValid )
        .set( false )(v0)
      updated( v2 )

    } else {
      noChange
    }
  }


  private def _emailLens =
    MRegS.s0Creds composeLens MReg0Creds.email
  private def _phoneLens =
    MRegS.s0Creds composeLens MReg0Creds.phone


  override protected def handle: PartialFunction[Any, ActionResult[M]] = {

    // Редактирования поля email.
    case m: RegEmailEdit =>
      _edit( _emailLens, m.email, Validators.isEmailValid )

    // Расфокусировка поля email. Надо проверить валидность поля.
    case RegEmailBlur =>
      _blur( _emailLens, Validators.isEmailValid )


    // Редактирования номера телефона.
    case m: RegPhoneEdit =>
      _edit( _phoneLens, m.phone, Validators.isPhoneValid )

    case RegPhoneBlur =>
      _blur( _phoneLens, Validators.isPasswordValid )

  }

}
