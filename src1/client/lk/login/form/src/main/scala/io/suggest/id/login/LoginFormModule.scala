package io.suggest.id.login

import com.softwaremill.macwire._
import io.suggest.id.login.c.IdentApiHttp
import io.suggest.id.login.m.LoginFormDiConfig
import io.suggest.id.login.v.epw.EpwFormR
import io.suggest.id.login.v.ext.ExtFormR
import io.suggest.id.login.v.pwch.{PwChangeR, PwNewR}
import io.suggest.id.login.v.reg.{Reg0CredsR, Reg1CaptchaR, Reg2SmsCodeR, Reg3CheckBoxesR, Reg4SetPasswordR, RegR}
import io.suggest.id.login.v.stuff.{CheckBoxR, ErrorSnackR, LoginProgressR, TextFieldR}
import io.suggest.id.login.v.{LoginFormCss, LoginFormR}
import io.suggest.spa.SioPages
import japgolly.scalajs.react.React
import japgolly.scalajs.react.extra.router.RouterCtl
import japgolly.scalajs.react.vdom.VdomElement

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 14.03.19 15:32
  * Description: DI для формы логина.
  */
trait LoginFormModuleBase {

  import io.suggest.ReactCommonModule._
  import io.suggest.lk.LkCommonModule._

  def loginFormCssCtx: React.Context[LoginFormCss]

  lazy val loginFormR = wire[LoginFormR]

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

  def loginRouterCtl: RouterCtl[SioPages.Login]

  def loginFormCircuit: LoginFormCircuit = wire[LoginFormCircuit]

  lazy val routerCtlCtx: React.Context[RouterCtl[SioPages.Login]] =
    React.createContext( loginRouterCtl )

  lazy val pwChangeCircuit = wire[PwChangeCircuit]

  lazy val identApi = wire[IdentApiHttp]

  def diConfig: LoginFormDiConfig

}
object LoginFormModuleBase {

  def circuit2loginCssRCtx(lfCircuitOpt: Option[LoginFormCircuit]) =
    React.createContext(
      lfCircuitOpt
        .map(_.overallRW.value.formCss)
        .orNull
    )

}


/** Отдельная форма со своим роутером и своими контекстами. */
final class LoginFormModule extends LoginFormModuleBase {

  override def diConfig = LoginFormDiConfig.Isolated

  lazy val loginFormSpaRouter: LoginFormSpaRouter = {
    new LoginFormSpaRouter(
      renderLoginFormF = {
        lazy val rendered: VdomElement =
          loginFormCircuit.wrap( identity(_) )( loginFormR.component.apply )
        loginFormPage =>
          loginFormCircuit.dispatch( loginFormPage )
          rendered
      },
    )
  }

  override def loginRouterCtl = loginFormSpaRouter.routerCtl

  override lazy val loginFormCircuit = super.loginFormCircuit

  override lazy val loginFormCssCtx: React.Context[LoginFormCss] =
    LoginFormModuleBase.circuit2loginCssRCtx( Some(loginFormCircuit) )

}
