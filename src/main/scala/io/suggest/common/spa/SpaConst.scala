package io.suggest.common.spa

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 15.12.16 17:29
  * Description: Константы для форм, организованных по
  */
object SpaConst {

  def PREFIX = "dd"

  /** id тега на странице, value которого содержит сериализованное состояние формы. */
  def STATE_CONT_ID = PREFIX + "s"

  /** id элемента-картинки, которая содержит красный анимированный прелоадер. */
  def PRE_LOADER_LK_DOM_ID = "pl_ri"

}
