package io.suggest.sc.m.dia

import io.suggest.ueq.UnivEqUtil._
import diode.FastEq
import io.suggest.sc.m.dia.first.MWzFirstOuterS
import japgolly.univeq.UnivEq
import monocle.macros.GenLens

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 18.01.19 12:14
  * Description: Контейнер данных диалогов.
  * Неявно-пустая модель.
  */
object MScDialogs {

  def empty = apply()

  implicit object MScDialogsFastEq extends FastEq[MScDialogs] {
    override def eqv(a: MScDialogs, b: MScDialogs): Boolean = {
      (a.first ===* b.first)
    }
  }

  @inline implicit def univEq: UnivEq[MScDialogs] = UnivEq.derive

  val first = GenLens[MScDialogs](_.first)

}


/** Модель-контейнер верхнего уровня для состояний диалогов.
  *
  * @param first Диалог первого запуска, когда открыт.
  */
case class MScDialogs(
                       first      : MWzFirstOuterS      = MWzFirstOuterS.empty
                     ) {

  def withFirst(first: MWzFirstOuterS) = copy(first = first)

}
