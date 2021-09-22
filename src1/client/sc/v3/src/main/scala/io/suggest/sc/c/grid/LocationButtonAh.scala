package io.suggest.sc.c.grid

import io.suggest.sjs.common.async.AsyncUtil.defaultExecCtx
import diode.{ActionHandler, ActionResult, Effect, ModelRW}
import io.suggest.conf.ConfConst
import io.suggest.i18n.MsgCodes
import io.suggest.log.Log
import io.suggest.msg.ErrorMsgs
import io.suggest.sc.index.MScIndexArgs
import io.suggest.sc.m.dia.err.MScErrorDia
import io.suggest.sc.m.dia.first.{InitFirstRunWz, MWzPhases}
import io.suggest.sc.m.inx.{GetIndex, GetIndexCancel, MScSwitchCtx}
import io.suggest.sc.m.{GeoLocTimerCancel, GeoLocTimerStart, MScRoot, RefreshCurrentLocation, SetErrorState, SettingAction}
import play.api.libs.json.{JsBoolean, JsNull}

/** Controller for handling location button actions. */
final class LocationButtonAh[M](
                                 modelRW: ModelRW[M, MScRoot],
                               )
  extends ActionHandler(modelRW)
  with Log
{

  override protected def handle: PartialFunction[Any, ActionResult[M]] = {

    // User requested for update current location information.
    case m @ RefreshCurrentLocation =>
      val fx = Effect.action {
        val locationEnabledKey = ConfConst.ScSettings.LOCATION_ENABLED
        SettingAction(
          key = locationEnabledKey,
          fx = {
            // Non-first run.
            case JsBoolean( isLocationEnabled ) =>
              val switchIndexQsArgs = MScIndexArgs(
                retUserLoc = false,
              )
              val finalSwitchCtx = MScSwitchCtx(
                indexQsArgs   = switchIndexQsArgs,
                demandLocTest = true,
              )

              val fx: Effect = if (isLocationEnabled) {
                // Location already enabled, this is normal flow. Make immediate answer and timer request.
                val afterSwitchFxSome = Some {
                  // If cancelled or index switching completed, also cancel geo-lock timer:
                  Effect.action( GeoLocTimerCancel ) +
                  Effect.action( GetIndexCancel )
                }
                GetIndex(
                  switchCtx = MScSwitchCtx(
                    indexQsArgs       = switchIndexQsArgs,
                    afterCancelSwitch = afterSwitchFxSome,
                    afterIndex        = afterSwitchFxSome,
                    demandLocTest     = true,
                  )
                ).toEffectPure + Effect.action {
                  GeoLocTimerStart(
                    finalSwitchCtx,
                    allowImmediate = false,
                    animateLocBtn = true,
                  )
                }
              } else {
                // Location disabled and/or not permitted. Only minimal fast first-step action.
                // TODO switchCtx: Render hint to open related geolocation settings, to alter geolocation state.
                GetIndex( finalSwitchCtx ).toEffectPure
              }

              Some( fx )

            // Setting value not yet defined. Possibly, first run. First, request permission.
            case JsNull =>
              val startPermRequest = Effect.action {
                InitFirstRunWz(
                  showHide    = true,
                  onlyPhases  = MWzPhases.GeoLocPerm :: Nil,
                  noAsk       = true,
                )
              }
              Some( startPermRequest )

            // Unexpected type of JSON-value: should not happen, because it should be boolean.
            case other =>
              logger.error( ErrorMsgs.SHOULD_NEVER_HAPPEN, msg = (m, locationEnabledKey, other) )
              val showErrorFx = Effect.action {
                SetErrorState(
                  MScErrorDia(
                    messageCode = MsgCodes.`Something.gone.wrong`,
                    retryAction = Some( m.asInstanceOf[RefreshCurrentLocation.type] ),
                  )
                )
              }
              Some( showErrorFx )
          },
        )
      }

      effectOnly( fx )

  }

}
