package io.suggest.spa

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

  object LkPreLoaderConst {

    /** id элемента-картинки, которая содержит красный анимированный прелоадер. */
    def DOM_ID = "pl_ri"

    def WIDTH_PX = 64
    def HEIGHT_PX = WIDTH_PX

    def CENTER_X = WIDTH_PX / 2
    def CENTER_Y = HEIGHT_PX / 2

  }

}
