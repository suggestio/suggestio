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

}
