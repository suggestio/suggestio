package io.suggest.lk.m

import diode.FastEq
import io.suggest.i18n.MMessage
import io.suggest.ueq.UnivEqUtil._
import japgolly.univeq.UnivEq
import monocle.macros.GenLens

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 21.09.17 12:14
  * Description: Состояние унифицированного попапа, выводящего ошибки.
  */
object MErrorPopupS {

  def empty = apply()

  /** Поддержка FastEq для [[MErrorPopupS]]. */
  implicit object MErrorPopupSFastEq extends FastEq[MErrorPopupS] {
    override def eqv(a: MErrorPopupS, b: MErrorPopupS): Boolean = {
      (a.messages ===* b.messages) &&
      (a.exception ===* b.exception)
    }
  }

  @inline implicit def univEq: UnivEq[MErrorPopupS] = UnivEq.derive

  def fromExOpt(exOpt: Option[Throwable]): Option[MErrorPopupS] = {
    for (_ <- exOpt) yield {
      apply(
        exception = exOpt
      )
    }
  }

  val messages  = GenLens[MErrorPopupS](_.messages)
  val exception = GenLens[MErrorPopupS](_.exception)

}


/** Класс-контейнер данных попапа, выводящего ошибки.
  *
  * @param messages Текстовые сообщения об ошибках, готовые к рендеру.
  * @param exception Исключение, если есть.
  */
case class MErrorPopupS(
                         messages     : List[MMessage]      = Nil,
                         exception    : Option[Throwable]   = None
                       )
