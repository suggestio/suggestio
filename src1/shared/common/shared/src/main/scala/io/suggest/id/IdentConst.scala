package io.suggest.id

import io.suggest.common.html.HtmlConstants

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 17.03.17 14:46
  * Description: Констансты подсистемы идентификации.
  */
object IdentConst {

  def HTTP_HDR_SUDDEN_AUTH_FORM_RESP = "X-Sio-Auth-Page"


  object Login {
    val NAME_FN       = "email"
    val PASSWORD_FN   = HtmlConstants.Input.password  // "password"
    val PHONE_FN      = "phone"
  }


  object Reg {

    /** Можно повторно отправлять смс после этого времени. */
    def SMS_CAN_RE_SEND_AFTER_SECONDS = 30

    /** Сколько времени валиден id-токен. */
    def ID_TOKEN_VALIDITY_SECONDS = 15 * 60

  }


  /** Данные по кукису, если его требуется отрабатывать вручную на клиенте (например - в cordova-приложении). */
  object CookieToken {
    def KV_STORAGE_TOKEN_KEY = "sc.login.session.cookie"
  }

}
