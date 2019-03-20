package io.suggest.id.login

import com.softwaremill.macwire._
import io.suggest.id.login.m.{ILoginFormPages, LoginProgressR}
import io.suggest.id.login.v.epw.{EpwFormR, EpwTextFieldR, ForeignPcCheckBoxR}
import io.suggest.id.login.v.ext.ExtFormR
import io.suggest.id.login.v.{LoginFormCss, LoginFormR, LoginFormSpaRouter}
import japgolly.scalajs.react.React
import japgolly.scalajs.react.extra.router.RouterCtl

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 14.03.19 15:32
  * Description: DI для формы логина.
  */
class LoginFormModule {

  import io.suggest.ReactCommonModule._

  def loginFormRF =
    () => wire[LoginFormR]

  lazy val epwFormR = wire[EpwFormR]
  lazy val foreignPcCheckBoxR = wire[ForeignPcCheckBoxR]
  lazy val epwTextFieldR = wire[EpwTextFieldR]

  lazy val extFormR = wire[ExtFormR]

  lazy val loginProgressR = wire[LoginProgressR]

  def loginFormCircuitF =
    (routerCtl: RouterCtl[ILoginFormPages]) => wire[LoginFormCircuit]

  lazy val loginFormSpaRouter = wire[LoginFormSpaRouter]

  lazy val loginFormRCtx: React.Context[LoginFormCss] =
    React.createContext( loginFormSpaRouter.circuit.overallRW.value.formCss )

}
