package io.suggest.sc.m.dia.err

import diode.FastEq
import diode.data.Pot
import io.suggest.msg.ErrorMsg_t
import io.suggest.spa.DAction
import japgolly.univeq._
import io.suggest.ueq.UnivEqUtil._
import io.suggest.ueq.JsUnivEqUtil._
import monocle.macros.GenLens

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 02.12.2019 18:54
  * Description: Опциональная модель данных диалога ошибки выдачи.
  */
object MScErrorDia {

  implicit object MScErrorDiaFastEq extends FastEq[MScErrorDia] {
    override def eqv(a: MScErrorDia, b: MScErrorDia): Boolean = {
      (a.messageCode    ===* b.messageCode) &&
      (a.pot            ===* b.pot) &&
      (a.hint           ===* b.hint) &&
      (a.retryAction    ===* b.retryAction)
    }
  }

  @inline implicit def univEq: UnivEq[MScErrorDia] = UnivEq.derive

  val messageCode   = GenLens[MScErrorDia](_.messageCode)
  val pot           = GenLens[MScErrorDia](_.pot)
  val hint          = GenLens[MScErrorDia](_.hint)
  val retryAction   = GenLens[MScErrorDia](_.retryAction)

}


/** Контейнер данных описания ошибки.
  *
  * @param messageCode Код ошибки.
  * @param retryAction Экшен для повторения.
  */
case class MScErrorDia(
                        messageCode    : ErrorMsg_t,
                        pot            : Pot[_]           = Pot.empty,
                        hint           : Option[String]   = None,
                        retryAction    : Option[DAction]  = None,
                      )
