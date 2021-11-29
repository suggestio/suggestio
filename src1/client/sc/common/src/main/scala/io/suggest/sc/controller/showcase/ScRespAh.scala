package io.suggest.sc.controller.showcase

import diode.{ActionHandler, ActionResult, Effect, ModelRW}
import io.suggest.sjs.common.async.AsyncUtil._
import io.suggest.common.coll.Lists.Implicits._
import io.suggest.common.empty.OptionUtil
import io.suggest.dev.{MScScreenS, MScreenInfo}
import io.suggest.i18n.MLanguage
import io.suggest.log.Log
import io.suggest.msg.{ErrorMsgs, JsonPlayMessages}
import io.suggest.sc.controller.{IRespActionHandler, IRespHandler, MRhCtx}
import io.suggest.sc.model.dev.MScDev
import io.suggest.sc.model.in.{MInternalInfo, MScInternals}
import io.suggest.sc.model.inx.{MScIndex, ReGetIndex}
import io.suggest.sc.model.{HandleScApiResp, MScRoot, OnlineCheckConn}
import io.suggest.sc.ssr.SsrSetState
import io.suggest.sc.view.styl.ScCss
import io.suggest.spa.DiodeUtil.Implicits._
import io.suggest.ueq.UnivEqUtil._
import japgolly.univeq._

import scala.util.Success

/** Controller for handling and sub-routing of showcase pub api responses. */
class ScRespAh(
                modelRW                  : ModelRW[MScRoot, MScRoot],
                scRespHandlers           : Seq[IRespHandler],
                scRespActionHandlers     : Seq[IRespActionHandler],
              )
  extends ActionHandler( modelRW )
  with Log
{ ah =>

  private def _handleScApiResp(m: HandleScApiResp, value0: MScRoot = value): ActionResult[MScRoot] = {
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


  override protected def handle: PartialFunction[Any, ActionResult[MScRoot]] = {
    // United processing for most of Sc-API-responses from server.
    // All responses for index, grid, focused, search actions - at first handled here.
    case m: HandleScApiResp =>
      _handleScApiResp( m )

    // Showcase running on server-side, and ScSite controller wants to render to HTML some stuff.
    // Rendering is synchronous, so showcase state & VDOM should be updated right now/ATM with no background tasks.
    case m: SsrSetState =>
      var v0 = value

      // Switch screen and ScCss to needed values.
      for {
        screen2 <- m.scQs.common.screen
        if v0.dev.screen.info.screen !=* screen2
      } {
        v0 = MScRoot.dev
          .andThen( MScDev.screen )
          .andThen( MScScreenS.info )
          .andThen( MScreenInfo.screen )
          .replace( screen2 )(v0)

        // ScCssRebuild minimalistic:
        val scCssArgs = MScRoot.scCssArgsFrom( v0 )
        if (v0.index.scCss.args != scCssArgs) {
          val scCss2 = ScCss( scCssArgs )
          val v2 = MScRoot.index
            .andThen( MScIndex.scCss )
            .replace( scCss2 )(v0)
          v0 = v2
        }
      }

      // Switch UI language, if langData is defined in original request:
      for {
        langData <- m.lang
        rctx_LENS = MScRoot.internals
          .andThen( MScInternals.info )
          .andThen( MInternalInfo.reactCtx )
        // This check must be done server-side (inside related actor/contoller), not here to obey server's decision about language.
        if !(rctx_LENS.get(v0).language contains[MLanguage] langData.lang)
        // Reset messages. Related React.Contexts will be re-rendered after final update(): ActionResult[].
        v2 = rctx_LENS.modify { rCtx0 =>
          rCtx0.copy(
            language = Some( langData.lang ),
            langSwitch = rCtx0.langSwitch.ready( JsonPlayMessages(langData.messagesMap) )
          )
        }(v0)
      } {
        v0 = v2
      }

      val m2 = HandleScApiResp(
        reqTimeStamp = None,
        qs = m.scQs,
        tryResp = Success( m.scResp ),
        reason = ReGetIndex(),  // TODO What should be defined as reason here?
      )
      _handleScApiResp( m2, v0 )

  }

}

/** Accumulator for folding response processing results: new state and effects accumulation. */
private case class RaFoldAcc(
                              v1         : MScRoot,
                              fxAccRev   : List[Effect]   = Nil
                            )
