package io.suggest.lk.tags.edit.m

import diode.FastEq
import diode.data.Pot
import io.suggest.common.tags.edit.MTagsEditProps
import io.suggest.common.tags.search.MTagsFound
import japgolly.univeq.UnivEq
import io.suggest.ueq.JsUnivEqUtil._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 27.12.16 13:54
  * Description: Несериализуемая client-only модель состояния редактора тегов.
  * Содержит всякие рантаймовые поля.
  */
object MTagsEditState {

  def empty = MTagsEditState()

  implicit object MTagsEditStateFastEq extends FastEq[MTagsEditState] {
    override def eqv(a: MTagsEditState, b: MTagsEditState): Boolean = {
      (a.props eq b.props) &&
        (a.found eq b.found) &&
        (a.searchTimer eq b.searchTimer)
    }
  }

  @inline implicit def univEq: UnivEq[MTagsEditState] = UnivEq.derive

}

/**
  * Класс модели рантаймового состояния поиска тегов.
  *
  * @param found Состояние поиска тегов.
  * @param searchTimer Таймер запуска поиска, если есть.
  *                    Появился из-за https://github.com/ochrons/diode/issues/37 в том числе.
  */
case class MTagsEditState(
                           props        : MTagsEditProps      = MTagsEditProps(),
                           found        : Pot[MTagsFound]     = Pot.empty,
                           searchTimer  : Option[Long]        = None
) {

  def withProps(props2: MTagsEditProps) = copy(props = props2)
  def withFound(found2: Pot[MTagsFound]) = copy(found = found2)
  def withSearchTimer(timer2: Option[Long]) = copy(searchTimer = timer2)

  def reset = MTagsEditState.empty.withProps(props)

}
