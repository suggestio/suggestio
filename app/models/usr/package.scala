package models

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

}
