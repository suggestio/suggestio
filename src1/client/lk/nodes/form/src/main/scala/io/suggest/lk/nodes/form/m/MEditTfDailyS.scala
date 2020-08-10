package io.suggest.lk.nodes.form.m

import diode.FastEq
import diode.data.Pot
import io.suggest.bill.tf.daily.{ITfDailyMode, MTfDailyInfo}
import japgolly.univeq.UnivEq
import monocle.macros.GenLens
import io.suggest.ueq.JsUnivEqUtil._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 21.03.17 21:48
  * Description: Модель состояния редактирования тарифа текущего узла.
  */
object MEditTfDailyS {

  /** Поддержка FastEq. */
  implicit object MTfDailyEditSFastEq extends FastEq[MEditTfDailyS] {
    override def eqv(a: MEditTfDailyS, b: MEditTfDailyS): Boolean = {
      (a.mode eq b.mode) &&
        (a.nodeTfOpt eq b.nodeTfOpt) &&
        (a.request eq b.request)
    }
  }

  def mode = GenLens[MEditTfDailyS](_.mode)
  def request = GenLens[MEditTfDailyS](_.request)
  def inputAmount = GenLens[MEditTfDailyS](_.inputAmount)

  @inline implicit def univEq: UnivEq[MEditTfDailyS] = UnivEq.derive

}


/** Класс модели данных состояния окна редактирование тарифа.
  *
  * @param mode Новый режим тарифа (пока не сохранён).
  * @param nodeTfOpt Данные по текущему тарифу узла.
  * @param request Реквест к серверу с обновлением тарифа.
  * @param inputAmount Текущее значение input'а, куда юзер вводит новый ценник.
  */
case class MEditTfDailyS(
                          mode              : ITfDailyMode,
                          nodeTfOpt         : Option[MTfDailyInfo],
                          inputAmount       : Option[MInputAmount]  = None,
                          request           : Pot[_]                = Pot.empty
                        ) {

  def withModeInputAmount(mode2: ITfDailyMode, inpAmount: Option[MInputAmount]) = {
    copy(
      mode          = mode2,
      inputAmount   = inpAmount
    )
  }

  /** Является ли текущее состояние тарифа пригодным для сохранения на сервере? */
  def isValid: Boolean = {
    mode.isValid &&
      inputAmount.fold(true)(_.isValid)
  }

}


case class MInputAmount(
                         value    : String,
                         isValid  : Boolean
                       )
object MInputAmount {
  @inline implicit def univEq: UnivEq[MInputAmount] = UnivEq.derive
}
