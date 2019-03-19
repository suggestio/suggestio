package io.suggest.id.login

import diode.react.ReactConnector
import io.suggest.id.login.c.{EpwAh, FormAh, ILoginApi, LoginApiHttp}
import io.suggest.id.login.m.MLoginRootS
import io.suggest.msg.ErrorMsgs
import io.suggest.sjs.common.log.CircuitLog
import io.suggest.spa.CircuitUtil._
import io.suggest.spa.DoNothingActionProcessor

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 14.03.19 15:14
  * Description: Цепочка для формы логина.
  */
class LoginFormCircuit
  extends CircuitLog[MLoginRootS]
  with ReactConnector[MLoginRootS]
{

  override protected def CIRCUIT_ERROR_CODE = ErrorMsgs.LOGIN_FORM_ERROR

  override protected def initialModel: MLoginRootS = {
    MLoginRootS()
  }


  private[login] val epwRW      = mkLensRootZoomRW(this, MLoginRootS.epw)
  private[login] val overallRW  = mkLensRootZoomRW(this, MLoginRootS.overall)


  val loginApi: ILoginApi = new LoginApiHttp

  private val formAh = new FormAh(
    modelRW = overallRW,
  )

  private val epwAh = new EpwAh(
    modelRW     = epwRW,
    loginApi    = loginApi,
  )

  override protected val actionHandler: HandlerFunction = {
    composeHandlers(
      epwAh,
      formAh
    )
  }

  addProcessor( DoNothingActionProcessor[MLoginRootS] )

}
