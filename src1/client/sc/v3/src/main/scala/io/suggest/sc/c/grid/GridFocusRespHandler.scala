package io.suggest.sc.c.grid

import diode.{ActionResult, Effect}
import diode.data.Pot
import io.suggest.sjs.common.async.AsyncUtil.defaultExecCtx
import io.suggest.jd.render.m.MJdDataJs
import io.suggest.msg.ErrorMsgs
import io.suggest.sc.c.{IRespWithActionHandler, MRhCtx}
import io.suggest.sc.m.{MScRoot, ResetUrlRoute, SetErrorState}
import io.suggest.sc.m.dia.err.MScErrorDia
import io.suggest.sc.m.grid.{GridBlockClick, MGridS, MScAdData}
import io.suggest.sc.sc3.{MSc3RespAction, MScRespActionType, MScRespActionTypes}
import io.suggest.log.Log
import io.suggest.ueq.JsUnivEqUtil._
import japgolly.univeq._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 04.03.2020 16:43
  * Description:
  */

/** Resp-handler для обработки ответа по фокусировке одной карточки. */
class GridFocusRespHandler
  extends IRespWithActionHandler
  with Log
{

  override def isMyReqReason(ctx: MRhCtx): Boolean = {
    ctx.m.reason.isInstanceOf[GridBlockClick]
  }

  override def getPot(ctx: MRhCtx): Option[Pot[_]] = {
    ctx.value0
      .grid.core
      .focusedAdOpt
      .map(_.focused)
  }

  override def handleReqError(ex: Throwable, ctx: MRhCtx): ActionResult[MScRoot] = {
    val eMsg = ErrorMsgs.XHR_UNEXPECTED_RESP
    logger.error(eMsg, ex, msg = ctx.m)
    val reason = ctx.m.reason.asInstanceOf[GridBlockClick]

    val errFx = Effect.action {
      val m = MScErrorDia(
        messageCode = eMsg,
        potRO       = Some(
          ctx.modelRW.zoom { mroot =>
            GridAh
              .findAd(reason.nodeId, mroot.grid.core)
              .fold( Pot.empty[MJdDataJs] )( _._1.focused )
          }
        ),
        retryAction = Some( ctx.m.reason ),
      )
      SetErrorState(m)
    }

    val g0 = ctx.value0.grid

    GridAh
      .findAd(reason.nodeId, g0.core)
      .fold[ActionResult[MScRoot]] {
        ActionResult.EffectOnly( errFx )
      } { case (ad0, index) =>
        val ad1 = MScAdData.focused
          .modify(_.fail(ex))(ad0)
        val g2 = GridAh.saveAdIntoValue(index, ad1, g0)
        val v2 = (MScRoot.grid set g2)( ctx.value0 )
        ActionResult.ModelUpdateEffect(v2, errFx)
      }
  }

  override def isMyRespAction(raType: MScRespActionType, ctx: MRhCtx): Boolean = {
    raType ==* MScRespActionTypes.AdsFoc
  }

  override def applyRespAction(ra: MSc3RespAction, ctx: MRhCtx): ActionResult[MScRoot] = {
    val focQs = ctx.m.qs.foc.get
    val nodeId = focQs.lookupAdId
    val g0 = ctx.value0.grid
    GridAh
      .findAd(nodeId, g0.core)
      .fold [ActionResult[MScRoot]] {
        logger.warn(ErrorMsgs.FOC_LOOKUP_MISSING_AD, msg = focQs)
        ActionResult.NoChange

      } { case (ad0, index) =>
        val ad1 = {
          val focResp = ra.ads.get
          val focAd = focResp.ads.head
          val focJdDataJs = MJdDataJs.fromJdData( focAd.jd, focAd.info )
          MScAdData.focused.modify( _.ready( focJdDataJs ) )(ad0)
        }
        // Обновление фокусировки:
        val gridCore2 = GridAh.resetFocus( index, ad1, g0.core )

        // Надо проскроллить выдачу на начало открытой карточки:
        val scrollFx = GridAh.scrollToAdFx( ad1, gridCore2.gridBuild )
        val resetRouteFx = ResetUrlRoute().toEffectPure

        val v2 = MScRoot.grid
          .composeLens( MGridS.core )
          .set( gridCore2 )( ctx.value0 )
        val fxOpt = Some(scrollFx + resetRouteFx)
        ActionResult(Some(v2), fxOpt)
      }
  }

}
