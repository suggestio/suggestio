package io.suggest.id.login

import diode.FastEq
import diode.react.ReactConnector
import io.suggest.id.login.c._
import io.suggest.id.login.m.MLoginRootS
import io.suggest.id.login.m.reg.MEpwRegS
import io.suggest.lk.c.{CaptchaAh, CaptchaApiHttp, ICaptchaApi}
import io.suggest.lk.m.CaptchaInit
import io.suggest.msg.ErrorMsgs
import io.suggest.sjs.common.log.CircuitLog
import io.suggest.spa.CircuitUtil._
import io.suggest.spa.{DoNothingActionProcessor, OptFastEq}
import japgolly.scalajs.react.extra.router.RouterCtl
import io.suggest.sjs.common.async.AsyncUtil.defaultExecCtx
import japgolly.univeq._

import scala.concurrent.{Future, Promise}

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


  private[login] val extRW      = mkLensRootZoomRW(this, MLoginRootS.ext)
  private[login] val epwRW      = mkLensRootZoomRW(this, MLoginRootS.epw)
  private[login] val overallRW  = mkLensRootZoomRW(this, MLoginRootS.overall)
  private[login] val returnUrlRO = overallRW.zoom(_.returnUrl)
  private[login] val epwRegRW   = mkLensRootZoomRW(this, MLoginRootS.epwReg)
  private[login] val captchaRW  = mkLensZoomRW( epwRegRW, MEpwRegS.captcha )( OptFastEq.Wrapped )


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

  private val epwRegAh = new EpwRegAh(
    modelRW     = epwRegRW,
    loginApi    = loginApi,
  )

  private val captchaAh = new CaptchaAh(
    modelRW     = captchaRW,
    api         = captchaApi,
  )

  override protected val actionHandler: HandlerFunction = {
    composeHandlers(
      formAh,
      epwAh,
      epwRegAh,
      extAh,
      captchaAh,
    )
  }

  addProcessor( DoNothingActionProcessor[MLoginRootS] )

  // При переключении на таб регистрации, надо инициализировать капчу первый раз:
  {
    val p = Promise[None.type]()
    val unSubscribeF = subscribe( overallRW.zoom(_.loginTab)(FastEq.AnyRefEq) ) { loginTabProxy =>
      if (loginTabProxy.value ==* MLoginTabs.EpwReg) {
        val isCaptchaNeedInit = epwRegRW.value.captcha
          .fold(true) { c => c.req.isEmpty && !c.req.isPending }
        if (isCaptchaNeedInit) {
          Future( dispatch(CaptchaInit) )
          p.success( None )
        }
      }
    }
    p.future.onComplete(_ => unSubscribeF())
  }

}
