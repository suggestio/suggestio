package io.suggest.captcha

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 14.05.15 16:06
 * Description: Клиент-серверные константы для формы ввода капчи и связанных с капчей вещей.
 */
object CaptchaConstants {

  /** id кнопки обновления капчи. */
  def CAPTCHA_RELOAD_BTN_ID = "captchaReload"

  /** Название аттрибута тега кнопки релоада, который содержит id картинки, для которой требуется перезагрузка. */
  def ATTR_RELOAD_FOR = "data-for"

  /** id инпута, в который юзер должен вводить значение капчи. */
  def CAPTCHA_TYPED_INPUT_ID = "captchaTyped"

  /** id контейнера со скрытой капчей. Скрытие обеспечено на уровне этого контейнера. */
  def HIDDEN_CAPTCHA_DIV_ID  = "captchaHidden"

  /** Чтобы браузер юзер не грузил капчу раньше времени, ссылка на капчу сохраняется в data-src. */
  def ATTR_HIDDEN_SRC = "data-src"

}
