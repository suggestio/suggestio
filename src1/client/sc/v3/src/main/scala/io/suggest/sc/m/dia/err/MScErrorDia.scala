package io.suggest.sc.m.dia.err

import diode.{FastEq, ModelR}
import diode.data.Pot
import io.suggest.msg.ErrorMsg_t
import io.suggest.sc.m.MScRoot
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
      (a.potRO          ===* b.potRO) &&
      (a.exceptionOpt   ===* b.exceptionOpt) &&
      (a.hint           ===* b.hint) &&
      (a.retryAction    ===* b.retryAction)
    }
  }

  @inline implicit def univEq: UnivEq[MScErrorDia] = UnivEq.derive

  val messageCode       = GenLens[MScErrorDia](_.messageCode)
  val potRO             = GenLens[MScErrorDia](_.potRO)
  val potUnSubscribe    = GenLens[MScErrorDia](_.potUnSubscribe)
  val exceptionOpt      = GenLens[MScErrorDia](_.exceptionOpt)
  val hint              = GenLens[MScErrorDia](_.hint)
  val retryAction       = GenLens[MScErrorDia](_.retryAction)


  implicit class ErrDiaExt(val sed: MScErrorDia) extends AnyVal {
    /** Узнать potRO...isPending.
      * Т.к. тут zoom, то нужно чтобы был def без шансов на val/lazy val.
      */
    def potIsPending: Boolean =
      sed.potRO.exists(_.value.isPending)

    def potOrEmpty: Pot[Any] =
      sed.potRO.fold( Pot.empty[Any] )(_.value)

  }

}


/** Контейнер данных описания ошибки.
  *
  * @param potRO Доступ к Pot'у, который описывает состояние запроса.
  * @param messageCode Код ошибки.
  * @param retryAction Экшен для повторения.
  * @param potUnSubscribe Заполняется контроллером диалога.
  * @param exceptionOpt Ошибка вне pot'а.
  */
case class MScErrorDia(
                        messageCode       : ErrorMsg_t,
                        potRO             : Option[ModelR[MScRoot, Pot[Any]]]  = None,
                        potUnSubscribe    : Option[() => Unit]         = None,
                        exceptionOpt      : Option[Throwable]          = None,
                        hint              : Option[String]             = None,
                        retryAction       : Option[DAction]            = None,
                      )
