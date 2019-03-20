package io.suggest.id.login

import com.softwaremill.macwire._
import io.suggest.id.login.m.LoginProgressR
import io.suggest.id.login.v.epw.{EpwFormR, ForeignPcCheckBoxR}
import io.suggest.id.login.v.ext.ExtFormR
import io.suggest.id.login.v.{LoginFormCss, LoginFormR}
import japgolly.scalajs.react.React

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

  lazy val extFormR = wire[ExtFormR]

  lazy val loginProgressR = wire[LoginProgressR]
  lazy val circuit = wire[LoginFormCircuit]

  lazy val loginFormRCtx: React.Context[LoginFormCss] = React.createContext( circuit.overallRW.value.formCss )

}
