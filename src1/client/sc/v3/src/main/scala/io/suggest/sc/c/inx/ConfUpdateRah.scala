package io.suggest.sc.c.inx

import diode.ActionResult
import io.suggest.sc.c.{IRespActionHandler, MRhCtx}
import io.suggest.sc.m.MScRoot
import io.suggest.sc.m.in.MScInternals
import io.suggest.sc.sc3.{MSc3Conf, MSc3RespAction, MScRespActionType, MScRespActionTypes}
import io.suggest.sc.u.Sc3ConfUtil
import io.suggest.sjs.common.log.Log
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
    val v0 = ctx.value0
    val conf0 = v0.internals.conf
    var conf2 = conf0

    // JSON-карта ресиверов:
    for {
      rcvrsMapUrlArgs2 <- action.rcvrsMap
      if rcvrsMapUrlArgs2 !=* conf0.rcvrsMapUrl
    } {
      conf2 = MSc3Conf.rcvrsMapUrl
        .set( rcvrsMapUrlArgs2 )(conf2)
      // TODO Организовать эффект или таймер для обновления карты. Таймер нужен, чтобы карта не обновлялась слишком часто.
    }

    // Если конфиг изменился, и у нас тут постоянная установка, то надо сохранить новый конфиг в состояние.
    if (conf0 ===* conf2) {
      ActionResult.ModelUpdate(v0)

    } else {
      // Конфиг изменился. Залить новый конфиг в состояние, запустить обновление и сохранение конфига, если необходимо.
      conf2 = MSc3Conf.clientUpdatedAt
        .set( Some(MSc3Conf.timestampSec()) )(conf2)

      // Заливка данных в состояние:
      val v2 = MScRoot.internals
        .composeLens( MScInternals.conf )
        .set( conf2 )( v0 )

      // Попытаться сохранить конфигурацию в постоянную модель:
      Sc3ConfUtil.saveInitIfPossible( v2.toScInit )

      // TODO Что с эффектом перезагрузки карты или иными возможными эффектами?

      ActionResult.ModelUpdate(v2)
    }
  }

}
