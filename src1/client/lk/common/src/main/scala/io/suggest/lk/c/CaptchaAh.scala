package io.suggest.lk.c

import diode.{ActionHandler, ActionResult, ModelRW}
import io.suggest.lk.m._
import io.suggest.lk.m.captcha.MCaptchaS
import japgolly.univeq._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 27.03.19 20:43
  * Description: Контроллер состояния капчи.
  */

class CaptchaAh[M](
                    modelRW       : ModelRW[M, MCaptchaS],
                  )
  extends ActionHandler( modelRW )
{

  override protected def handle: PartialFunction[Any, ActionResult[M]] = {

    // Капчу проинициализировать/переинициализировать:
    case CaptchaInit =>
      val v0 = value

      val captchaId = MCaptchaS.mkCaptchaId()

      val v2 = v0.reset( Some(captchaId) )
      updated(v2)


    // Скрыть капчу, если она отображается.
    case CaptchaHide =>
      val v0 = value
      if (v0.captchaId.isEmpty) {
        noChange
      } else {
        val v2 = v0.reset( None )
        updated( v2 )
      }


    // Ввод значения капчи.
    case m: CaptchaTyped =>
      val v0 = value

      if (v0.typed.value ==* m.captchaId) {
        // Текст капчи не изменился.
        noChange
      } else if (v0.captchaId contains m.captchaId) {
        var updAccF = MTextFieldS.value.set( m.typed )
        if (!v0.typed.isValid && MCaptchaS.isTypedCapchaValid(v0.typed.value))
          updAccF = updAccF andThen MTextFieldS.isValid.set(true)

        val v2 = MCaptchaS.typed
          .modify( updAccF )(v0)

        updated(v2)
      } else {
        // Почему-то id капчи не совпадает. Вероятно, капча изменилась во время ввода или что-то ещё.
        noChange
      }


    // Сигнал разфокусировки инпута капчи.
    case CaptchaInputBlur =>
      // Проверить, есть ли валидный текст на капче.
      val v0 = value

      var inputChangesAccF = List.empty[MTextFieldS => MTextFieldS]

      // Поверхностная проверка введённого текста капчи.
      if (
        v0.typed.isValid &&
        !MCaptchaS.isTypedCapchaValid(v0.typed.value)
      ) {
        // Капча стала невалидной.
        inputChangesAccF ::= MTextFieldS.isValid.set( false )
      }

      // Тримминг начала и хвоста капчи.
      if (v0.typed.value.nonEmpty) {
        val trimmedTyped = v0.typed.value.trim
        if ( trimmedTyped !=* v0.typed.value )
          inputChangesAccF ::= MTextFieldS.value.set( trimmedTyped )
      }

      inputChangesAccF
        .reduceOption(_ andThen _)
        .fold(noChange) { updateF =>
          val v2 = MCaptchaS.typed.modify( updateF )(v0)
          updated(v2)
        }

  }

}
