package io.suggest.sc.c.showcase

import diode.{ActionHandler, ActionResult, Effect, ModelRW}
import io.suggest.sjs.common.async.AsyncUtil._
import io.suggest.common.coll.Lists.Implicits._
import io.suggest.common.empty.OptionUtil
import io.suggest.log.Log
import io.suggest.msg.ErrorMsgs
import io.suggest.sc.c.{IRespActionHandler, IRespHandler, MRhCtx}
import io.suggest.sc.m.{HandleScApiResp, MScRoot, OnlineCheckConn}
import io.suggest.spa.DiodeUtil.Implicits._
import io.suggest.ueq.UnivEqUtil._

/** Controller for handling and sub-routing of showcase pub api responses. */
class ScRespAh(
                modelRW                  : ModelRW[MScRoot, MScRoot],
                scRespHandlers           : Seq[IRespHandler],
                scRespActionHandlers     : Seq[IRespActionHandler],
              )
  extends ActionHandler( modelRW )
  with Log
{ ah =>

  override protected def handle: PartialFunction[Any, ActionResult[MScRoot]] = {

    // United processing for most of Sc-API-responses from server.
    // All responses for index, grid, focused, search actions - at first handled here.
    case m: HandleScApiResp =>
      val value0 = value

      val rhCtx0 = MRhCtx(value0, m, modelRW)
      val respHandler = scRespHandlers
        .find { rh =>
          rh.isMyReqReason(rhCtx0)
        }
        .get

      // At first, compare request and Pot timestamps, if any.
      val rhPotOpt = respHandler.getPot(rhCtx0)
      val isActualResp = m.reqTimeStamp.fold(true) { reqTimeStamp =>
        rhPotOpt
          .exists(_ isPendingWithStartTime reqTimeStamp)
      }

      // Two-way processing logic over Either. Lfet for error result. Right for normal processing.
      (for {
        _ <- Either.cond(
          test = isActualResp,
          left = {
            logger.log(ErrorMsgs.SRV_RESP_INACTUAL_ANYMORE, msg = (respHandler.getClass.getSimpleName, rhPotOpt.flatMap(_.pendingOpt).map(_.startTime), m.reqTimeStamp) )
            noChange
          },
          right = None,
        )

        // Process tryResp.
        scResp <- {
          // Save request error into state:
          for (ex <- m.tryResp.toEither.left) yield {
            val actionRes0 = respHandler.handleReqError(ex, rhCtx0)
            // Append OnLineCheckConn effect (check if error related to lost internet connection).
            var fxAcc = actionRes0.effectOpt.toList

            if (OnlineCheckConn.maybeNeedCheckConnectivity(ex))
              fxAcc ::= OnlineCheckConn.toEffectPure

            val withOnlineCheckFx = fxAcc.mergeEffects
            ActionResult( actionRes0.newModelOpt, withOnlineCheckFx )
          }
        }

      } yield {
        val acc9 = scResp.respActions.foldLeft( RaFoldAcc(value0) ) { (acc0, ra) =>
          val rhCtx1 = (MRhCtx.value0 replace acc0.v1)(rhCtx0)
          scRespActionHandlers
            .find { rah =>
              rah.isMyRespAction( ra.acType, rhCtx0 )
            }
            .map { rah =>
              rah.applyRespAction( ra, rhCtx1 )
            }
            .fold {
              // This response action is NOT supported by current showcase version.
              // Or by logical error, if there are showcase resp.action type without RespActionHandler implemented for such type.
              logger.error( ErrorMsgs.SHOULD_NEVER_HAPPEN, msg = ra )
              acc0
            } { actRes =>
              acc0.copy(
                v1        = actRes.newModelOpt getOrElse acc0.v1,
                fxAccRev  = actRes.effectOpt :?: acc0.fxAccRev,
              )
            }
        }

        // Fast equality to check, if something changed.
        val v9Opt = OptionUtil.maybe(acc9.v1 !===* value0)( acc9.v1 )

        // Merge all accumulated effects.
        val fxOpt9 = acc9.fxAccRev
          .reverse
          .mergeEffects

        ah.optionalResult( v9Opt, fxOpt9 )
      })
        // Return Left or Right results.
        .fold(identity, identity)

  }

}

/** Accumulator for folding response processing results: new state and effects accumulation. */
private case class RaFoldAcc(
                              v1         : MScRoot,
                              fxAccRev   : List[Effect]   = Nil
                            )
