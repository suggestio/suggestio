package controllers

import play.api.data.Form

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 27.01.15 16:19
 */
package object ident {

  type EmailPwLoginForm_t = Form[(String, String)]

}
