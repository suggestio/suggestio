package io.suggest.lk.nodes.form.m

import diode.FastEq
import diode.data.Pot
import io.suggest.bill.tf.daily.{ITfDailyMode, MTfDailyInfo}

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

  def withMode(mode2: ITfDailyMode) = copy(mode = mode2)
  def withRequest(req2: Pot[_]) = copy(request = req2)

  def withModeInputAmount(mode2: ITfDailyMode, inpAmount: Option[MInputAmount]) = {
    copy(
      mode          = mode2,
      inputAmount   = inpAmount
    )
  }

  def isValid = mode.isValid && (inputAmount.isEmpty || inputAmount.exists(_.isValid))

}


case class MInputAmount(
                         value    : String,
                         isValid  : Boolean
                       )
