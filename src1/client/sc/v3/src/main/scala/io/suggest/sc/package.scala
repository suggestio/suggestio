package io.suggest

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 05.06.2020 13:14
  */
package object sc {

  /** Доп.API для модуля, чтобы не трогать внутренний scope, и не влиять на wire-макросы. */
  implicit final class Sc3ModuleExt( private val module: Sc3Module.type ) extends AnyVal {

    def sc3Circuit = module.sc3SpaRouter.sc3Circuit

  }

}
