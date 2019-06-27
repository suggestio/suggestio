package io.suggest.id.login

import com.softwaremill.macwire._
import io.suggest.id.login.v.epw.EpwFormR
import io.suggest.id.login.v.ext.ExtFormR
import io.suggest.id.login.v.reg.{Reg0CredsR, Reg1CaptchaR, Reg2SmsCodeR, Reg3CheckBoxesR, Reg4SetPasswordR, RegR}
import io.suggest.id.login.v.stuff.{ButtonR, CheckBoxR, LoginProgressR, TextFieldR}
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
  lazy val textFieldR = wire[TextFieldR]
  lazy val regR = wire[RegR]
  lazy val reg0CredsR = wire[Reg0CredsR]
  lazy val reg1CaptchaR = wire[Reg1CaptchaR]
  lazy val reg2SmsCodeR = wire[Reg2SmsCodeR]
  lazy val reg3CheckBoxesR = wire[Reg3CheckBoxesR]
  lazy val reg4SetPasswordR = wire[Reg4SetPasswordR]

  lazy val extFormR = wire[ExtFormR]

  lazy val checkBoxR = wire[CheckBoxR]
  lazy val loginProgressR = wire[LoginProgressR]
  lazy val buttonR = wire[ButtonR]

  def loginFormCircuitF =
    (routerCtl: RouterCtl[ILoginFormPages]) => wire[LoginFormCircuit]

  lazy val loginFormSpaRouter = wire[LoginFormSpaRouter]

  lazy val loginFormCssCtx: React.Context[LoginFormCss] =
    React.createContext( loginFormSpaRouter.circuit.overallRW.value.formCss )

}
