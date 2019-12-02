package io.suggest.sc.m.dia.err

import diode.FastEq
import io.suggest.spa.DAction
import japgolly.univeq._
import io.suggest.ueq.UnivEqUtil._
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
      (a.messageCode ===* b.messageCode) &&
      (a.retry ===* b.retry)
    }
  }

  @inline implicit def univEq: UnivEq[MScErrorDia] = UnivEq.derive

  val messageCode   = GenLens[MScErrorDia](_.messageCode)
  val retry         = GenLens[MScErrorDia](_.retry)

}


/** Контейнер данных описания ошибки.
  *
  * @param messageCode Код ошибки
  * @param retry Экшен для повторения.
  */
case class MScErrorDia(
                        messageCode     : String,
                        retry           : DAction,
                      )
