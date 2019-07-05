package io.suggest.id.login

import diode.react.ReactConnector
import io.suggest.id.login.c._
import io.suggest.id.login.c.pwch.SetNewPwAh
import io.suggest.id.login.c.reg.{Reg0CredsAh, Reg3CheckBoxesAh, RegAh}
import io.suggest.id.login.m.epw.MEpwLoginS
import io.suggest.id.login.m.ext.MExtLoginFormS
import io.suggest.id.login.m.pwch.MPwNew
import io.suggest.id.login.m.{MLoginFormOverallS, MLoginRootS}
import io.suggest.id.login.m.reg.MRegS
import io.suggest.id.login.m.reg.step0.MReg0Creds
import io.suggest.id.login.m.reg.step1.MReg1Captcha
import io.suggest.id.login.m.reg.step2.MReg2SmsCode
import io.suggest.id.login.m.reg.step3.MReg3CheckBoxes
import io.suggest.id.login.m.reg.step4.MReg4SetPassword
import io.suggest.lk.c.{CaptchaAh, CaptchaApiHttp, ICaptchaApi, SmsCodeFormAh}
import io.suggest.lk.m.captcha.MCaptchaS
import io.suggest.lk.m.sms.MSmsCodeS
import io.suggest.msg.ErrorMsgs
import io.suggest.sjs.common.log.CircuitLog
import io.suggest.spa.CircuitUtil._
import io.suggest.spa.{DoNothingActionProcessor, OptFastEq}
import japgolly.scalajs.react.extra.router.RouterCtl
import io.suggest.sjs.common.async.AsyncUtil.defaultExecCtx
import japgolly.univeq._


/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 14.03.19 15:14
  * Description: Цепочка для формы логина.
  */
class LoginFormCircuit(
                        routerCtl: RouterCtl[ILoginFormPages]
                      )
  extends CircuitLog[MLoginRootS]
  with ReactConnector[MLoginRootS]
{

  override protected def CIRCUIT_ERROR_CODE = ErrorMsgs.LOGIN_FORM_ERROR

  override protected def initialModel: MLoginRootS = {
    MLoginRootS()
  }


  private[login] val extRW          = mkLensRootZoomRW(this, MLoginRootS.ext)( MExtLoginFormS.MExtLoginFormSFastEq )
  private[login] val epwRW          = mkLensRootZoomRW(this, MLoginRootS.epw)( MEpwLoginS.MEpwLoginSFastEq )
  private[login] val overallRW      = mkLensRootZoomRW(this, MLoginRootS.overall)( MLoginFormOverallS.MLoginFormOverallSFastEq )
  private[login] val returnUrlRO    = overallRW.zoom(_.returnUrl)
  private[login] val pwNewRW        = mkLensZoomRW( overallRW, MLoginFormOverallS.pwNew )( MPwNew.MPwNewFastEq )

  private[login] val regRW          = mkLensRootZoomRW(this, MLoginRootS.reg)( MRegS.MEpwRegSFastEq )
  private[login] val reg0CredsRW    = mkLensZoomRW( regRW, MRegS.s0Creds )( MReg0Creds.MReg0CredsFastEq )

  private[login] val reg1CaptchaRW  = mkLensZoomRW( regRW, MRegS.s1Captcha )( MReg1Captcha.MReg1CaptchaFastEq )
  private[login] val captchaOptRW   = mkLensZoomRW( reg1CaptchaRW, MReg1Captcha.captcha )( OptFastEq.Wrapped(MCaptchaS.MCaptchaSFastEq) )

  private[login] val reg2SmsCodeRW  = mkLensZoomRW( regRW, MRegS.s2SmsCode )( MReg2SmsCode.MReg2SmsCodeFastEq )
  private[login] val smsCodeFormRW  = mkLensZoomRW( reg2SmsCodeRW, MReg2SmsCode.smsCode )( OptFastEq.Wrapped(MSmsCodeS.MSmsCodeSFastEq) )

  private[login] val reg3CheckBoxesRW   = mkLensZoomRW(regRW, MRegS.s3CheckBoxes)( MReg3CheckBoxes.MReg3CheckBoxesFastEq )
  private[login] val reg4SetPasswordRW  = mkLensZoomRW(regRW, MRegS.s4SetPassword)( MReg4SetPassword.MReg4SetPasswordFastEq )


  val loginApi: ILoginApi = new LoginApiHttp
  val captchaApi: ICaptchaApi = new CaptchaApiHttp

  private val formAh = new FormAh(
    modelRW   = overallRW,
    routerCtl = routerCtl,
  )

  private val extAh = new ExtAh(
    modelRW     = extRW,
    returnUrlRO = returnUrlRO,
  )

  private val epwAh = new EpwAh(
    modelRW     = epwRW,
    loginApi    = loginApi,
    returnUrlRO = returnUrlRO,
  )

  private val captchaAh = new CaptchaAh(
    modelRW     = captchaOptRW,
    api         = captchaApi,
    idTokenRO   = reg0CredsRW.zoom( _.submitReq.get.token ),
  )


  private val regAh = new RegAh(
    modelRW  = regRW,
    loginApi = loginApi,
    pwNewRO  = pwNewRW,
  )

  private val reg0CredsAh = new Reg0CredsAh(
    modelRW   = regRW,
    routerCtl = routerCtl,
  )

  private val reg3CheckBoxesAh = new Reg3CheckBoxesAh(
    modelRW = reg3CheckBoxesRW
  )

  private val setNewPwAh = new SetNewPwAh(
    modelRW = pwNewRW,
  )

  private val smsCodeFormAh = new SmsCodeFormAh(
    modelRW = smsCodeFormRW
  )

  override protected val actionHandler: HandlerFunction = {
    composeHandlers(
      formAh,
      epwAh,
      regAh, reg0CredsAh, reg3CheckBoxesAh, setNewPwAh,
      extAh,
      captchaAh,
      smsCodeFormAh,
    )
  }

  addProcessor( DoNothingActionProcessor[MLoginRootS] )

  // При переключении на таб регистрации, надо инициализировать капчу первый раз:
  /*
  {
    val p = Promise[None.type]()
    val unSubscribeF = subscribe( overallRW.zoom(_.loginTab)(FastEq.AnyRefEq) ) { loginTabProxy =>
      if (loginTabProxy.value ==* MLoginTabs.EpwReg) {
        val isCaptchaNeedInit = reg1Captcha.value.captcha
          .fold(true) { c => c.contentReq.isEmpty && !c.contentReq.isPending }
        if (isCaptchaNeedInit) {
          Future( dispatch(CaptchaInit) )
          p.success( None )
        }
      }
    }
    p.future.onComplete(_ => unSubscribeF())
  }
  */

}
