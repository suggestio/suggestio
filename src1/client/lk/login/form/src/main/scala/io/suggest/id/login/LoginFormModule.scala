package io.suggest.id.login

import com.softwaremill.macwire._
import io.suggest.id.login.v.epw.EpwFormR
import io.suggest.id.login.v.ext.ExtFormR
import io.suggest.id.login.v.pwch.{PwChangeR, PwNewR}
import io.suggest.id.login.v.reg.{Reg0CredsR, Reg1CaptchaR, Reg2SmsCodeR, Reg3CheckBoxesR, Reg4SetPasswordR, RegR}
import io.suggest.id.login.v.stuff.{CheckBoxR, ErrorSnackR, LoginProgressR, TextFieldR}
import io.suggest.id.login.v.{LoginFormCss, LoginFormR, LoginFormSpaRouter}
import io.suggest.spa.SioPages
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

  lazy val errorSnackR = wire[ErrorSnackR]
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

  lazy val pwNewR = wire[PwNewR]
  lazy val pwChangeR = wire[PwChangeR]

  def loginFormCircuitF =
    (routerCtl: RouterCtl[SioPages]) => wire[LoginFormCircuit]

  lazy val loginFormSpaRouter = wire[LoginFormSpaRouter]

  lazy val loginFormCssCtx: React.Context[LoginFormCss] =
    React.createContext( loginFormSpaRouter.circuit.overallRW.value.formCss )
  lazy val routerCtlRctx: React.Context[RouterCtl[SioPages]] =
    React.createContext( loginFormSpaRouter.routerCtl )


  lazy val pwChangeCircuit = wire[PwChangeCircuit]

}
