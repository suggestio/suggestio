package io.suggest.adn.edit.m

import diode.FastEq
import diode.data.Pot
import io.suggest.lk.m.color.MColorsState
import japgolly.univeq._
import io.suggest.ueq.UnivEqUtil._
import io.suggest.ueq.JsUnivEqUtil._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 17.04.18 21:18
  * Description: Модель служебных внутренних данных для формы lk-adn-edit.
  */
object MAdnEditInternals {

  /** Поддержка FastEq. */
  implicit object MAdnEditInternalsFastEq extends FastEq[MAdnEditInternals] {
    override def eqv(a: MAdnEditInternals, b: MAdnEditInternals): Boolean = {
      (a.saving ===* b.saving) &&
      (a.conf ===* b.conf) &&
      (a.colorState ===* b.colorState)
    }
  }

  @inline implicit def univEq: UnivEq[MAdnEditInternals] = UnivEq.derive

}


/** Класс-контейнер внутренних данных.
  *
  * @param conf Конфиг формы.
  * @param saving Состояние сохранение формы.
  * @param colorState Состояние цветов для контроллера ColorPickAh и color-picker'ов формы.
  */
case class MAdnEditInternals(
                              conf          : MAdnEditFormConf,
                              saving        : Pot[MAdnEditForm] = Pot.empty,
                              colorState    : MColorsState      = MColorsState.empty,
                            ) {

  def withSaving(saving: Pot[MAdnEditForm]) = copy(saving = saving)
  def withColorState(colorState: MColorsState) = copy(colorState = colorState)

}
