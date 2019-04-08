package io.suggest.id.login

import com.softwaremill.macwire._
import io.suggest.id.login.v.epw.{EpwFormR, EpwTextFieldR}
import io.suggest.id.login.v.ext.ExtFormR
import io.suggest.id.login.v.reg.{EpwRegR, RegFinishR}
import io.suggest.id.login.v.stuff.{ButtonR, CheckBoxR, LoginProgressR}
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
  import io.suggest.lk.LkCommonModule._

  def loginFormRF =
    () => wire[LoginFormR]

  lazy val epwFormR = wire[EpwFormR]
  lazy val epwTextFieldR = wire[EpwTextFieldR]
  lazy val epwRegR = wire[EpwRegR]

  lazy val extFormR = wire[ExtFormR]

  lazy val checkBoxR = wire[CheckBoxR]
  lazy val loginProgressR = wire[LoginProgressR]
  lazy val buttonR = wire[ButtonR]

  lazy val regFinishR = wire[RegFinishR]

  def loginFormCircuitF =
    (routerCtl: RouterCtl[ILoginFormPages]) => wire[LoginFormCircuit]

  lazy val loginFormSpaRouter = wire[LoginFormSpaRouter]

  lazy val loginFormRCtx: React.Context[LoginFormCss] =
    React.createContext( loginFormSpaRouter.circuit.overallRW.value.formCss )

}