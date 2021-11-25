package io.suggest.sc.controller.dev

import diode.{ActionHandler, ActionResult, Effect, ModelRW}
import diode.data.{Pot, Ready}
import io.suggest.sjs.common.async.AsyncUtil.defaultExecCtx
import io.suggest.common.empty.OptionUtil
import io.suggest.perm.BoolOptPermissionState
import io.suggest.sc.ScConstants
import io.suggest.sc.controller.showcase.ScRoutingAh
import io.suggest.sc.index.MScIndexArgs
import io.suggest.sc.model.dia.first.{MWzPhases, WzPhasePermRes}
import io.suggest.sc.model.{GeoLocTimeOut, GeoLocTimerCancel, GeoLocTimerStart, GlPubSignal, MScRoot}
import io.suggest.sc.model.in.{MGeoLocTimerData, MInternalInfo, MScInternals}
import io.suggest.sc.model.inx.{GetIndex, MScSwitchCtx}
import io.suggest.sc.model.search.MMapInitState
import io.suggest.spa.DiodeUtil.Implicits._
import io.suggest.sjs.dom2.DomQuick
import io.suggest.spa.DoNothing
import io.suggest.ueq.JsUnivEqUtil._
import japgolly.univeq._

import scala.util.Success


/** Showcase contoller for geolocation timer tasks. */
class ScGeoTimerAh[M](
                       modelRW: ModelRW[M, MScRoot],
                     )
  extends ActionHandler( modelRW )
{ ah =>

  /** Remove timer from state, and clear timer effect. */
  private def _removeTimer(v0: MScRoot) = {
    for (glt <- v0.internals.info.geoLockTimer.toOption) yield {
      val alterF = MScRoot.internals
        .andThen( MScInternals.info )
        .andThen( MInternalInfo.geoLockTimer )
        .replace( Pot.empty.unavailable() )
      val fx = _clearTimerFx( glt.timerId )
      (alterF, fx)
    }
  }

  /** Effect for geo-timer timer cancelling. */
  private def _clearTimerFx(timerId: Int): Effect = {
    Effect.action {
      DomQuick.clearTimeout( timerId )
      DoNothing
    }
  }

  /** Start new geo-location timer and provide state changes.
    *
    * @return Updated state and effect for timer await.
    */
  def _mkGeoLocTimer(glTimerStart: GeoLocTimerStart, currTimerPot: Pot[MGeoLocTimerData]): (MScInternals => MScInternals, Effect) = {
    // TODO impure: timer started outside effect.
    val tp = DomQuick.timeoutPromiseT( ScConstants.ScGeo.INIT_GEO_LOC_TIMEOUT_MS )( GeoLocTimeOut )
    val modifier = MScInternals.info
      .andThen( MInternalInfo.geoLockTimer )
      .replace(
        Ready(
          MGeoLocTimerData(
            timerId = tp.timerId,
            reason = glTimerStart,
          )
        )
          .pending()
      )

    var timeoutFx: Effect = Effect( tp.fut )
    for (currTimer <- currTimerPot) {
      timeoutFx += _clearTimerFx( currTimer.timerId )
    }

    (modifier, timeoutFx)
  }


  override protected def handle: PartialFunction[Any, ActionResult[M]] = {

    // React for geolocation detected action.
    case m: GlPubSignal =>
      val v0 = value

      var modsAcc = List.empty[MScRoot => MScRoot]
      var fxAcc = List.empty[Effect]
      var nonSilentUpdate = false
      // TODO Bypass initial flag via info.geoLockTimer.reason? do not do any detection here.
      val mapNeedReset = v0.index.resp.isEmpty || v0.dialogs.first.isVisible

      val geoLocTimerDataOpt0 = v0.internals.info.geoLockTimer
      for {
        // TODO But if no timer started (geoLocTimer==Pot.empty), what to do?
        geoLockTimer <- geoLocTimerDataOpt0
        glSignal <- m.origOpt
        if glSignal.glType.isHighAccuracy
      } {
        fxAcc ::= Effect.action {
          val indexMapReset2 = !mapNeedReset
          val scSwitch0 = geoLockTimer.reason.switchCtx
          val switchCtx2 = if (mapNeedReset) {
            (
              (MScSwitchCtx.indexMapReset replace indexMapReset2) andThen
              MScSwitchCtx.indexQsArgs
                .andThen( MScIndexArgs.retUserLoc )
                .modify( _ || !m.origOpt.exists(_.isSuccess) )
            )(scSwitch0)
          } else {
            scSwitch0
          }

          GetIndex( switchCtx2 )
        }

        for {
          (alterF, ctFx) <- _removeTimer(v0)
        } {
          modsAcc ::= alterF
          fxAcc ::= ctFx
        }
      }

      // Update location on the map.
      // TODO Convert map alterings to effects, routed into other controllers? If so, this controller may be model-isolated into Pot[MGeoLocTimerData], instead of MScRoot.
      for {
        glSignal <- m.origOpt
        geoLoc <- glSignal.locationOpt
      } {
        // Always save coordinates to map .userLoc
        var mapInitModF = MMapInitState.userLoc replace Some(geoLoc)

        if (
          // Do NOT change map center, if this is just background demand index test.
          !geoLocTimerDataOpt0.exists(_.reason.switchCtx.demandLocTest) && ((
            // If current position is very awaited by initialization, then move map center to geoposition.
            (
              !v0.dialogs.first.isViewFinished ||
              (v0.index.resp ==* Pot.empty)
            ) &&
            (v0.internals.info.currRoute.exists { currRoute =>
              currRoute.locEnv.isEmpty && currRoute.nodeId.isEmpty
            })
          ) || (
            // Reset map state to detected coord during early stage. ReIndex will be called in from WzFirstAh#InitFirstRunWz(false).
            v0.index.resp.isEmpty ||
            v0.dialogs.first.isVisible ||
            // geoIntoRcrv: For example, right after wzFirst, location timer is started, and map position must obey to any geolocation.
            geoLocTimerDataOpt0.exists( _.reason.switchCtx.indexQsArgs.geoIntoRcvr )
          ))
        ) {
          mapInitModF = mapInitModF andThen MMapInitState
            .state.modify( _.withCenterInitReal(geoLoc.point) )
          nonSilentUpdate = true
        } else {
          nonSilentUpdate = nonSilentUpdate || v0.index.search.panel.opened
        }

        modsAcc ::= MScRoot.index
          .andThen( ScRoutingAh._inxSearchGeoMapInitLens )
          .modify( mapInitModF )
      }

      // If wzFirst waiting for geolocaiton permission, lets notify him
      // TODO This is still needed? Need to review about WzFirstAh/IPermissionSpec.listenChangesOfPot state monitoring should be enought.
      if (
        v0.dialogs.first.isVisible &&
          v0.dialogs.first.perms
            .get( MWzPhases.GeoLocPerm )
            .exists(_.isPending)
      ) {
        fxAcc ::= WzPhasePermRes(
          phase = MWzPhases.GeoLocPerm,
          res = Success( BoolOptPermissionState(OptionUtil.SomeBool.someTrue) ),
          startTimeMs = None,
        )
          .toEffectPure
      }

      ah.optionalResult(
        v2Opt = modsAcc
          .reduceOption(_ andThen _)
          .map(_(v0)),
        fxOpt = fxAcc.mergeEffects,
        silent = !nonSilentUpdate,
      )


    // Timeout waiting for geolocation data changes. Lets start re-indexing using current data.
    case GeoLocTimeOut =>
      val v0 = value
      v0.internals.info.geoLockTimer.fold(noChange) { glTimerData =>
        // Удалить из состояния таймер геолокации, запустить выдачу.
        var fx: Effect = GetIndex( glTimerData.reason.switchCtx ).toEffectPure

        val v2 = _removeTimer(v0)
          .fold(v0) { case (alterF, cancelTimerFx) =>
            fx += cancelTimerFx
            alterF( v0 )
          }

        for (onCompleteFxF <- glTimerData.reason.onComplete)
          fx += onCompleteFxF( false )

        updated( v2, fx )
      }


    // Start geolocation timer action.
    case m: GeoLocTimerStart =>
      val v0 = value
      val currLocOpt = v0.dev.geoLoc.currentLocation

      if (m.allowImmediate && currLocOpt.exists(_._1.isHighAccuracy)) {
        // Timer not needed, already have enought geo.data.
        var fx: Effect = GetIndex( m.switchCtx ).toEffectPure
        for (onCompleteFxF <- m.onComplete)
          fx += onCompleteFxF(true)
        effectOnly( fx )

      } else {
        val (viMod, fx) = _mkGeoLocTimer( m, v0.internals.info.geoLockTimer )
        val v2 = MScRoot.internals.modify(viMod)(v0)
        updated( v2, fx )
      }

    // Cancel geolocation timer
    case GeoLocTimerCancel =>
      val v0 = value

      _removeTimer( v0 )
        .fold(noChange) { case (modF, fx) =>
          updated( modF(v0), fx )
        }

  }

}
