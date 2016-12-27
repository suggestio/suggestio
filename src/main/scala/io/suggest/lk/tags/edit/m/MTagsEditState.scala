package io.suggest.lk.tags.edit.m

import diode.FastEq
import diode.data.Pot
import io.suggest.common.tags.search.MTagsFound

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 27.12.16 13:54
  * Description: Несериализуемая client-only модель состояния редактора тегов.
  * Содержит всякие рантаймовые поля.
  */
object MTagsEditState {

  def empty = MTagsEditState()

  implicit object MTagsEditFastEq extends FastEq[MTagsEditState] {
    override def eqv(a: MTagsEditState, b: MTagsEditState): Boolean = {
      (a.found eq b.found) &&
        (a.searchTimer eq b.searchTimer)
    }
  }

}

/**
  * Класс модели рантаймового состояния поиска тегов.
  *
  * @param found Состояние поиска тегов.
  * @param searchTimer Таймер запуска поиска, если есть.
  *                    Появился из-за https://github.com/ochrons/diode/issues/37 в том числе.
  */
case class MTagsEditState(
                           found        : Pot[MTagsFound]   = Pot.empty,
                           searchTimer  : Option[Long]       = None
) {

  def withFound(found2: Pot[MTagsFound]) = copy(found = found2)
  def withSearchTimer(timer2: Option[Long]) = copy(searchTimer = timer2)

  def reset = MTagsEditState.empty

}
