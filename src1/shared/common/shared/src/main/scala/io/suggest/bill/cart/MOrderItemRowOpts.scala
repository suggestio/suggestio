package io.suggest.bill.cart

import diode.UseValueEq
import japgolly.univeq._
import play.api.libs.json._
import play.api.libs.functional.syntax._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 19.09.18 14:46
  * Description: Модель пропертисов для рядов и заголовка.
  */
object MOrderItemRowOpts {

  /** Поддержка play-json. */
  implicit def mOrderItemsRowOptsFormat: OFormat[MOrderItemRowOpts] = (
    (__ \ "s").format[Boolean] and
    (__ \ "c").format[Boolean]
  )(apply, unlift(unapply))

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
{

  def toAddColsCount: Int = {
    var cc = 0
    if (withStatus)
      cc += 1
    if (withCheckBox)
      cc += 1
    cc
  }

}
