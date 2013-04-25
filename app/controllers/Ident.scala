package controllers

import play.api.Play.current
import play.api.libs.ws.WS
import util.AclT

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 25.04.13 11:47
 * Description: Контроллер обычного логина в систему. Обычно логинятся через Mozilla Persona, но не исключено, что
 * в будущем будет также и вход по имени/паролю для некоторых учетных записей.
 */

object Ident extends AclT {

  // URL, используемый для person'a. Если сие запущено на локалхосте, то надо менять этот адресок.
  val AUDIENCE_URL_DEFAULT = "https://suggest.io:443"
  //val AUDIENCE_URL_DEFAULT = "http://localhost:9000"
  val AUDIENCE_URL = current.configuration.getString("persona.audience.url").getOrElse(AUDIENCE_URL_DEFAULT)

  val VERIFIER_URL = "https://verifier.login.persona.org/verify"


}
