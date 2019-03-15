package io.suggest.id.login

import com.softwaremill.macwire._
import io.suggest.id.login.v.{EpwFormR, ForeignPcCheckBoxR, LoginFormR}

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 14.03.19 15:32
  * Description: DI для формы логина.
  */
class LoginFormModule {

  import io.suggest.ReactCommonModule._


  lazy val loginFormR = wire[LoginFormR]
  lazy val epwFormR = wire[EpwFormR]
  lazy val foreignPcCheckBoxR = wire[ForeignPcCheckBoxR]

  lazy val circuit = wire[LoginFormCircuit]

}
