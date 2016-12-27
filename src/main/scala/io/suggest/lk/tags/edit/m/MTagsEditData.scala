package io.suggest.lk.tags.edit.m

import io.suggest.common.tags.edit.MTagsEditProps

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 27.12.16 16:29
  * Description: Контейнер всех моделей TagsEdit.
  * Нужно для ActionHandler'а, возможно для каких-либо ещё вьюшек.
  */
case class MTagsEditData(
  props   : MTagsEditProps,
  state   : MTagsEditState
) {

  def withProps(props2: MTagsEditProps) = copy(props = props2)
  def withState(state2: MTagsEditState) = copy(state = state2)

}
