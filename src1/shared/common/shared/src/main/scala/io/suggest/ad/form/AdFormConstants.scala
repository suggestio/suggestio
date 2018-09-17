package io.suggest.ad.form

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 07.05.15 18:41
 * Description: Константы формы создания/редактирования рекламной карточки, расшаренные между sjs и web21.
 */
object AdFormConstants {

  // Названия корневых полей формы редактирования.
  def OFFER_K     = "offer"


  /** Константы для запуска детектора основных цветов картинок. */
  object ColorDetect {

    /** Размер генерируемой палитры. */
    def PALETTE_SIZE = 8

  }


  // v2: react-form

  /** id контейнера react-формы создания/редактирования рекламной карточки. */
  def AD_EDIT_FORM_CONT_ID = "aef"

}
