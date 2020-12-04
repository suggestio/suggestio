package io.suggest.sc.c.inx

import diode.ActionResult
import io.suggest.sc.c.{IRespActionHandler, MRhCtx}
import io.suggest.sc.m.{MScRoot, SaveConf}
import io.suggest.sc.sc3.{MSc3RespAction, MScRespActionType, MScRespActionTypes}
import io.suggest.log.Log
import io.suggest.sjs.common.async.AsyncUtil.defaultExecCtx
import japgolly.univeq._
import io.suggest.ueq.UnivEqUtil._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 14.11.18 15:54
  * Description: Обработчик resp-action'ов для обновления конфигурации.
  */
class ConfUpdateRah
  extends IRespActionHandler
  with Log
{

  override def isMyRespAction(raType: MScRespActionType, ctx: MRhCtx): Boolean = {
    raType ==* MScRespActionTypes.ConfUpdate
  }


  override def applyRespAction(ra: MSc3RespAction, ctx: MRhCtx): ActionResult[MScRoot] = {
    val action = ra.confUpdate.get

    ActionResult.EffectOnly( SaveConf(Some(action)).toEffectPure )
  }

}
