package io.suggest.captcha

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 14.05.15 16:06
 * Description: Клиент-серверные константы для формы ввода капчи и связанных с капчей вещей.
 */
object CaptchaConstants {

  /** Название http-заголовка с секретом капчи. */
  def CAPTCHA_SECRET_HTTP_HDR_NAME = "X-Sio-Captcha"

}
