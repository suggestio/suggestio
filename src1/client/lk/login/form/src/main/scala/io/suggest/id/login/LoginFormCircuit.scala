package io.suggest.id.login

import diode.react.ReactConnector
import io.suggest.id.login.c._
import io.suggest.id.login.m.{ILoginFormPages, MLoginRootS}
import io.suggest.msg.ErrorMsgs
import io.suggest.sjs.common.log.CircuitLog
import io.suggest.spa.CircuitUtil._
import io.suggest.spa.DoNothingActionProcessor
import japgolly.scalajs.react.extra.router.RouterCtl

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


  val loginApi: ILoginApi = new LoginApiHttp

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

  override protected val actionHandler: HandlerFunction = {
    composeHandlers(
      extAh,
      epwAh,
      formAh
    )
  }

  addProcessor( DoNothingActionProcessor[MLoginRootS] )

}
