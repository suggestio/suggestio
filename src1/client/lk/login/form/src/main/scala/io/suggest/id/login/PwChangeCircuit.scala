package io.suggest.id.login

import diode.FastEq
import diode.react.ReactConnector
import io.suggest.id.login.c.{IIdentApi, IdentApiHttp}
import io.suggest.id.login.c.pwch.{PasswordInputAh, PwChangeAh, SetNewPwAh}
import io.suggest.id.login.m.LoginFormDiConfig
import io.suggest.id.login.m.pwch.{MPwChangeRootS, MPwChangeS, MPwNew}
import io.suggest.lk.m.input.MTextFieldS
import io.suggest.msg.{ErrorMsg_t, ErrorMsgs}
import io.suggest.log.CircuitLog
import io.suggest.proto.http.model.HttpClientConfig
import io.suggest.spa.CircuitUtil._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 09.07.19 16:19
  * Description: Цепь для формы смены пароля.
  */
class PwChangeCircuit(
                       identApi: IIdentApi,
                     )
  extends CircuitLog[MPwChangeRootS]
  with ReactConnector[MPwChangeRootS]
{

  override protected def CIRCUIT_ERROR_CODE: ErrorMsg_t = ErrorMsgs.LOGIN_FORM_ERROR

  override protected def initialModel: MPwChangeRootS = {
    MPwChangeRootS.empty
  }


  private[login] val rootRO  = zoom(identity)( FastEq.AnyRefEq )

  private val formRW  = mkLensRootZoomRW( this, MPwChangeRootS.form )( MPwChangeS.MPwChangeSFastEq )
  private val pwNewRW = mkLensZoomRW( formRW, MPwChangeS.pwNew )( MPwNew.MPwNewFastEq )
  private val oldPwRW = mkLensZoomRW( formRW, MPwChangeS.pwOld )( MTextFieldS.MEpwTextFieldSFastEq )

  private val setNewPwAh = new SetNewPwAh(
    modelRW = pwNewRW,
  )

  private val currPasswordInputAh = new PasswordInputAh(
    modelRW = oldPwRW,
  )

  private val pwChangeAh = new PwChangeAh(
    identApi     = identApi,
    modelRW = formRW,
  )

  override protected val actionHandler: HandlerFunction = {
    composeHandlers(
      setNewPwAh,
      currPasswordInputAh,
      pwChangeAh,
    )
  }

}
