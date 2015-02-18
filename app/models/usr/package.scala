package models

import play.api.data.Form

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 02.02.15 11:46
 */
package object usr {

  /** Тип экземпляра данных SecureSocial, вокруг которого крутится весь API SecureSocial. */
  type SsUser = MExtIdent

  /** Тип экземпляра статической модели [[IdProviders]]. */
  type IdProvider = IdProviders.T

  /** Тип подмодели ident. */
  type MPersonIdentType = IdTypes.T

  /** Тип формы для логина по email+пароль. */
  type EmailPwLoginForm_t   = Form[EpwLoginFormBind]
}
