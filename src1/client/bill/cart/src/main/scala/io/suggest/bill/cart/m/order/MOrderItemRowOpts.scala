package io.suggest.bill.cart.m.order

import diode.UseValueEq
import japgolly.univeq._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 19.09.18 14:46
  * Description: Модель пропертисов для рядов и заголовка.
  */
object MOrderItemRowOpts {

  implicit def univEq: UnivEq[MOrderItemRowOpts] = UnivEq.derive

}


/** Класс-контейнер данных по рядам.
  *
  * @param withStatus Рендерить колонку статуса?
  * @param withCheckBox Рендерить чекбокс выделения ряда?
  */
case class MOrderItemRowOpts(
                              withStatus    : Boolean,
                              withCheckBox  : Boolean,
                              // TODO count-столбец для количества покупок?
                            )
  extends UseValueEq
