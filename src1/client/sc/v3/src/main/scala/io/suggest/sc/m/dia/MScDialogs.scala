package io.suggest.sc.m.dia

import io.suggest.ueq.UnivEqUtil._
import diode.FastEq
import io.suggest.sc.m.dia.first.MWzFirstOuterS
import japgolly.univeq.UnivEq

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 18.01.19 12:14
  * Description: Контейнер данных диалогов
  */
object MScDialogs {

  def empty = apply()

  implicit object MScDialogsFastEq extends FastEq[MScDialogs] {
    override def eqv(a: MScDialogs, b: MScDialogs): Boolean = {
      (a.first ===* b.first)
    }
  }

  implicit def univEq: UnivEq[MScDialogs] = UnivEq.derive

}


/** Модель-контейнер верхнего уровня для состояний диалогов.
  *
  * @param first Диалог первого запуска, когда открыт.
  */
case class MScDialogs(
                       first: Option[MWzFirstOuterS] = None
                     ) {

  def withFirst(first: Option[MWzFirstOuterS]) = copy(first = first)

}
