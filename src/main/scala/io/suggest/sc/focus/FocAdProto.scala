package io.suggest.sc.focus

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 11.06.15 18:36
 * Description: Константы протокола для моделей focused-рендера карточек.
 */
object FocAdProto {

  /** Имя поля с ES id карточки. */
  def MAD_ID_FN         = "a"

  /** Имя поля с html-рендером тела focused-карточки. */
  def BODY_HTML_FN      = "b"

  /** Имя поля с html-рендером focused заголовка, стрелочек и прочего контента, не участвующего
    * в слайд-анимации данных карточки. */
  def CONTROLS_HTML_FN  = "c"

  /** Имя поля, содержащего id продьюсера рекламной карточки. */
  def PRODUCER_ID_FN    = "d"

  /** Имя поля с человеческим порядковым номером. */
  def INDEX_FN          = "e"

}
