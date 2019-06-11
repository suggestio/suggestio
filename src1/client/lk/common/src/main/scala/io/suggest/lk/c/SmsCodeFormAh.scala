package io.suggest.lk.c

import diode.{ActionHandler, ActionResult, ModelRW}
import io.suggest.lk.m.{SmsCodeBlur, SmsCodeSet}
import io.suggest.lk.m.input.MTextFieldS
import io.suggest.lk.m.sms.MSmsCodeS
import monocle.Traversal
import scalaz.std.option._
import japgolly.univeq._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 11.06.19 21:43
  * Description: Контроллер для формы проверки смс-кода.
  */
object SmsCodeFormAh {

  private def _isSmsCodeValid(smsCode: String): Boolean = {
    smsCode.nonEmpty
  }

}


class SmsCodeFormAh[M](
                        modelRW: ModelRW[M, Option[MSmsCodeS]]
                      )
  extends ActionHandler( modelRW )
{

  private val typedL = Traversal
    .fromTraverse[Option, MSmsCodeS]
    .composeLens( MSmsCodeS.typed )


  override protected def handle: PartialFunction[Any, ActionResult[M]] = {

    // Редактирование поля ввода смс-кода.
    case m: SmsCodeSet =>
      val v0Opt = value
      if (typedL.composeLens(MTextFieldS.value).exist(_ ==* m.smsCode)(v0Opt) ) {
        noChange
      } else {
        val v2Opt = typedL
          .modify { t0 =>
            t0.copy(
              value   = m.smsCode,
              isValid = SmsCodeFormAh._isSmsCodeValid(m.smsCode)
            )
          }(v0Opt)
        updated( v2Opt )
      }


    // Расфокусировка поля ввода смс-кода
    case SmsCodeBlur =>
      val v0Opt = value
      if (typedL.exist { t0 => t0.isValid !=* SmsCodeFormAh._isSmsCodeValid(t0.value) }(v0Opt)) {
        val v2Opt = typedL
          .composeLens(MTextFieldS.isValid)
          .set(false)(v0Opt)
        updated(v2Opt)
      } else {
        noChange
      }

  }

}
