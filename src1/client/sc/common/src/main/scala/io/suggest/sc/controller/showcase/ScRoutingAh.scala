package io.suggest.sc.controller.showcase

import diode._
import diode.data.Pot
import io.suggest.common.empty.OptionUtil
import io.suggest.log.Log
import io.suggest.sc.controller.dia.WzFirstDiaAh
import io.suggest.sc.index.MScIndexArgs
import io.suggest.sc.model._
import io.suggest.sc.model.boot.{Boot, BootAfter, MBootServiceIds}
import io.suggest.sc.model.dia.first.InitFirstRunWz
import io.suggest.sc.model.grid._
import io.suggest.sc.model.in.{MInternalInfo, MJsRouterS, MScInternals}
import io.suggest.sc.model.inx._
import io.suggest.sc.model.menu.DlAppOpen
import io.suggest.sc.model.search.{MGeoTabS, MMapInitState, MScSearch, NodeRowClick}
import io.suggest.sjs.common.async.AsyncUtil.defaultExecCtx
import io.suggest.spa.DiodeUtil.Implicits._
import io.suggest.spa.{DoNothing, SioPages}
import io.suggest.ueq.JsUnivEqUtil._
import io.suggest.ueq.UnivEqUtil._
import japgolly.scalajs.react.extra.router.RouterCtl
import japgolly.univeq._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 25.07.17 12:53
  * Description: Outer SPA URL-router interaction controller.
  */
object ScRoutingAh {

  /** Make current state data for URL.
    * @param v0 MScRoot instance.
    * @return Sc3 state URL snapshot.
    */
  def getMainScreenSnapShot(v0: MScRoot): SioPages.Sc3 = {
    val inxState = v0.index.state
    val searchOpened = v0.index.search.panel.opened

    // If nothing in view (showcase not yet fully loaded), do not mess original values.
    val bootingRoute = v0.internals.info.currRoute
      .filter( _ => inxState.viewCurrent.isEmpty && v0.index.resp.isEmpty )
    val currRcvrId = bootingRoute.fold(inxState.rcvrId)(_.nodeId)

    // TODO Support for several tags inside URL
    val selTagIdOpt = v0.index.search.geo.data.selTagIds.headOption

    // Do not render coords in URL, if showcase index contexted in identified node, closed search panel and no selected tag.
    // This helps for caching, also.
    val locEnv2 = OptionUtil.maybe {
      currRcvrId.isEmpty
    } {
      bootingRoute
        .flatMap(_.locEnv)
        .getOrElse {
          v0.index.search.geo.mapInit.state.center
        }
    }

    SioPages.Sc3(
      nodeId        = currRcvrId,
      locEnv        = locEnv2,
      generation    = Some( inxState.generation ),
      searchOpened  = searchOpened,
      tagNodeId     = selTagIdOpt,
      menuOpened    = v0.index.menu.opened,
      focusedAdId   = for {
        scAdLoc   <- v0.grid.core.ads.interactAdOpt
        scAd      = scAdLoc.getLabel
        adData    <- scAd.data.toOption
        if adData.isOpened
        nodeId    <- adData.doc.tagId.nodeId
      } yield {
        nodeId
      },
      firstRunOpen = v0.dialogs.first.view.nonEmpty,
      dlAppOpen    = v0.index.menu.dlApp.opened,
      settingsOpen = v0.dialogs.settings.opened,
      login = for {
        loginCircuit <- v0.dialogs.login.ident
      } yield {
        loginCircuit.currentPage()
      },
    )
  }

  def _inxSearchGeoMapInitLens = {
    MScIndex.search
      .andThen( MScSearch.geo )
      .andThen( MGeoTabS.mapInit )
  }


  def root_internals_info_currRoute_LENS = {
    MScRoot.internals
      .andThen( MScInternals.info )
      .andThen( MInternalInfo.currRoute )
  }


  def root_index_search_geo_init_state_LENS = MScRoot.index
    .andThen( _inxSearchGeoMapInitLens )
    .andThen( MMapInitState.state )

}


/** Controller class for SPA router interaction. */
final class ScRoutingAh(
                         routerCtl       : RouterCtl[SioPages.Sc3],
                         modelRW         : ModelRW[MScRoot, MScRoot],
                       )
  extends ActionHandler(modelRW)
  with Log
{ ah =>

  override protected def handle: PartialFunction[Any, ActionResult[MScRoot]] = {

    // SPA-router notifies about possible changes inside current page URL:
    case m: RouteTo =>
      val v0 = value

      // Mods accumulater for apply against original state.
      var modsAcc = List.empty[MScRoot => MScRoot]

      // Possibly, js-роутер or cordova-fetch no yet ready, and it is needed to delay full processing.
      // Awaiting for cordova-fetch is needed, because cordova (AFNetworking) on iOS 12.x is initializing too long.
      val isFullyReady = v0.dev.platform.isReady &&
        v0.internals.jsRouter.jsRouter.isReady &&
        !v0.dialogs.first.isVisible

      // Is need to retry, when js-router become ready?
      var jsRouterAwaitRoute = false

      val isBootCompleted = v0.internals.boot.isBootCompleted
      val isBootCompletedOrForce = m.force || isBootCompleted

      // Grab current Sc3 state URL snapshot.
      val currMainScreen = ScRoutingAh.getMainScreenSnapShot( v0 )

      var isToReloadIndex = isFullyReady && v0.index.resp.isTotallyEmpty
      var needUpdateUi = false
      var isGeoLocRunning = v0.internals.info.geoLockTimer.nonEmpty
      var isGridNeedsReload = false

      // Effects accumulator:
      var fxsAcc = List.empty[Effect]

      // Save m.route into state, if route is different from changed value.
      val currRouteLens = ScRoutingAh.root_internals_info_currRoute_LENS
      if ( !(currRouteLens.get(v0) contains[SioPages.Sc3] m.mainScreen) )
        modsAcc ::= currRouteLens.replace( Some(m.mainScreen) )

      // Check .generation field changes:
      for {
        generation2 <- m.mainScreen.generation
        if !(currMainScreen.generation contains[Long] generation2)
      } {
        modsAcc ::= MScRoot.index
          .andThen(MScIndex.state)
          .andThen(MScIndexState.generation)
          .replace(generation2)
        // generation is changed. Reload grid:
        if (isFullyReady) {
          val adsPot = v0.grid.core.ads.adsTreePot
          if (adsPot.isPending || adsPot.exists(!_.subForest.isEmpty))
            isGridNeedsReload = true
        } else {
          jsRouterAwaitRoute = true
        }
      }

      // Check .searchOpened value:
      if (m.mainScreen.searchOpened !=* currMainScreen.searchOpened) {
        // First sidebar opening may cause requests to jsRouter (may be not yet ready).
        var sideBarFx: Effect = SideBarOpenClose(
          bar = MScSideBars.Search,
          open = OptionUtil.SomeBool(m.mainScreen.searchOpened),
        ).toEffectPure

        // So, check readyness of jsRouter, before open:
        if (v0.dialogs.first.isViewNotStarted) {
          val jsRouterSvcId = MBootServiceIds.JsRouter
          sideBarFx = Boot( jsRouterSvcId :: Nil ).toEffectPure >>
            BootAfter( jsRouterSvcId, sideBarFx ).toEffectPure
        }
        fxsAcc ::= sideBarFx
      }

      // Check id of current node:
      if (m.mainScreen.nodeId !=* currMainScreen.nodeId) {
        if (isFullyReady) {
          isToReloadIndex = true
          needUpdateUi = true
          // nodeId will be passed via action.
        } else {
          jsRouterAwaitRoute = true
        }
      }

      // Check for coordinates changes of current geo.point:
      if (
        // If current geo.point state changed:
        !(
          (m.mainScreen.locEnv.isEmpty && currMainScreen.locEnv.isEmpty) ||
          m.mainScreen.locEnv.exists { mgp2 =>
            currMainScreen.locEnv.exists { mgp0 =>
              mgp0 ~= mgp2
            }
          }
        )
      ) {
        for (nextGeoPoint <- m.mainScreen.locEnv) {
          modsAcc ::= ScRoutingAh.root_index_search_geo_init_state_LENS
            .modify {
              _.withCenterInitReal( nextGeoPoint )
            }
        }
        if (isFullyReady) {
          needUpdateUi = true
          isToReloadIndex = true
        } else {
          jsRouterAwaitRoute = true
        }
      }

      // Compare currently selected tag id:
      for {
        tagNodeId <- m.mainScreen.tagNodeId
        if !(currMainScreen.tagNodeId contains[String] tagNodeId)
      } {
        if (isFullyReady)
          fxsAcc ::= NodeRowClick( tagNodeId ).toEffectPure
        else
          jsRouterAwaitRoute = true
      }

      // Check menu panel state: opened/closed?
      if (m.mainScreen.menuOpened !=* currMainScreen.menuOpened)
        fxsAcc ::= SideBarOpenClose( MScSideBars.Menu, OptionUtil.SomeBool(m.mainScreen.menuOpened) ).toEffectPure

      // Compare focused ad id:
      if (
        (m.mainScreen.focusedAdId !=* currMainScreen.focusedAdId) &&
        !isToReloadIndex
      ) {
        if (isFullyReady) {
          val focusedAdIdOpt = m.mainScreen.focusedAdId
            .orElse( currMainScreen.focusedAdId )
          if (focusedAdIdOpt.nonEmpty) {
            // For GridBlockClick action, gridKey calculation is needed. Search ad-node with such nodeId in current grid:
            fxsAcc ::= GridBlockClick(
              adIds = focusedAdIdOpt.toList,
              gridPath = None,
              gridKey = None,
            ).toEffectPure
          }
        } else {
          jsRouterAwaitRoute = true
        }
      }

      if (m.mainScreen.dlAppOpen !=* currMainScreen.dlAppOpen)
        fxsAcc ::= DlAppOpen( opened = m.mainScreen.dlAppOpen ).toEffectPure

      if (m.mainScreen.settingsOpen !=* currMainScreen.settingsOpen)
        fxsAcc ::= SettingsDiaOpen( opened = m.mainScreen.settingsOpen ).toEffectPure

      // If virtual beacons defined in state, lets reload grid.
      if (m.mainScreen.virtBeacons !=* currMainScreen.virtBeacons)
        isGridNeedsReload = true

      // If no geo-point and no nodeId, it is needed to start geolocation timer cycle.
      if (
        isBootCompleted &&
        m.mainScreen.needGeoLoc &&
        v0.dialogs.first.isViewWasStarted &&
        v0.dialogs.first.view.isEmpty &&
        !WzFirstDiaAh.isNeedWizardFlowVal
      ) {
        // If geolocation not yet started, start it:
        fxsAcc ::= Effect.action {
          PeripheralStartStop(
            isStart = OptionUtil.SomeBool.someTrue,
            pauseResume = true,
          )
        }
      }

      // If need delay, push current route into delayed. If not, clear delayed route value.
      val root_internals_jsRouter_delayedRouteTo_LENS = MScRoot.internals
        .andThen( MScInternals.jsRouter )
        .andThen( MJsRouterS.delayedRouteTo )
      val delayedRouteTo0 = root_internals_jsRouter_delayedRouteTo_LENS get v0
      for {
        awaitRouteOpt <- {
          // Set this route delayment:
          if (jsRouterAwaitRoute)
            Some(Some(m))
          // Reset previously route set:
          else if (delayedRouteTo0.nonEmpty)
            Some(None)
          // If nothing to change, do nothing here:
          else None
        }
      } {
        modsAcc ::= root_internals_jsRouter_delayedRouteTo_LENS replace awaitRouteOpt

        // If delayed-route set first time, need to subscribe to BootAfter(JsRouter), after platform ready.
        if (delayedRouteTo0.isEmpty) {
          val afterRouterReadyFx = Effect.action {
            // When platform ready and jsRouter ready, let's dispatch delayed-route action.
            // In cordova iOS-12.x jsRouter is ready before platformReady, but vice-versa on others.
            val runRouteFx = Effect.action {
              root_internals_jsRouter_delayedRouteTo_LENS
                .get( modelRW.value )
                .getOrElse( DoNothing )
            }
            // Saved delayed route will be cleared after applying it in next RouteTo(). (see below delayedRouteTo.nonEmpty).
            BootAfter(
              MBootServiceIds.JsRouter,
              fx = runRouteFx,
              ifMissing = Some( runRouteFx ),
            )
          }
          fxsAcc ::= BootAfter(
            MBootServiceIds.Platform,
            fx = afterRouterReadyFx,
            ifMissing = Some( afterRouterReadyFx ),
          )
            .toEffectPure
        }
      }

      // First-run dialog opened? TODO Delete it? because wz1 now living in notification snack and becoming implicit/transparent!
      if (currMainScreen.firstRunOpen !=* m.mainScreen.firstRunOpen)
        // Start dialog open/close action, if needed:
        fxsAcc ::= InitFirstRunWz( m.mainScreen.firstRunOpen ).toEffectPure

      // Login dialog opened? Open, if needed. Also, check if user already logged in by now.
      val login2 = m.mainScreen.login
        .filter(_ => !v0.index.isLoggedIn)
      if (currMainScreen.login !=* login2)
        fxsAcc ::= ScLoginFormChange( login2 ).toEffectPure

      // Make updated state value, if any changes exists.
      val v2Opt = modsAcc
        .reduceOption(_ andThen _)
        .map(_(v0))

      if (isToReloadIndex && !isGeoLocRunning && isBootCompletedOrForce) {
        // Full showcase node-index reloading:
        val switchCtx = MScSwitchCtx(
          showWelcome = m.mainScreen.showWelcome,
          indexQsArgs = MScIndexArgs(
            geoIntoRcvr = m.mainScreen.nodeId.isEmpty && (v0.index.resp ==* Pot.empty),
            retUserLoc  = v0.index.search.geo.mapInit.userLoc.isEmpty,
            nodeId      = m.mainScreen.nodeId,
          ),
          focusedAdId = m.mainScreen.focusedAdId,
        )
        fxsAcc ::=
          GetIndex( switchCtx ).toEffectPure >>
          LoadIndexRecents(clean = false).toEffectPure

      } else if (isGridNeedsReload && isBootCompletedOrForce) {
        // Current showcase index don't need to reload, but reload grid is needed.
        fxsAcc ::= GridLoadAds( clean = true, ignorePending = true ).toEffectPure
      }

      ah.optionalResult(v2Opt, fxsAcc.mergeEffects, silent = needUpdateUi)


    // Action to make a SPA-router to update current URL:
    case m: ResetUrlRoute =>
      val v0 = value
      def nextRoute0 = ScRoutingAh.getMainScreenSnapShot( v0 )
      var nextRoute2 = m.mods.fold(nextRoute0)( _(() => nextRoute0) )

      val currRouteLens = ScRoutingAh.root_internals_info_currRoute_LENS
      val currRouteOpt = currRouteLens.get( v0 )

      if (!m.force && (currRouteOpt contains[SioPages.Sc3] nextRoute2)) {
        noChange
      } else {
        // Copy URL-field of virtBeacons, if change inside same location.
        for {
          currRoute <- currRouteOpt
          if currRoute.virtBeacons.nonEmpty &&
            (currRoute isSamePlaceAs nextRoute2)
        }
          nextRoute2 = (SioPages.Sc3.virtBeacons replace currRoute.virtBeacons)(nextRoute2)

        // Notify router-ctl about change:
        var fx: Effect = Effect.action {
          routerCtl
            .set( nextRoute2, m.via )
            .runNow()
          DoNothing
        }

        // Force flag must be forwarded explicitly via RouteTo, because it is used to bypass some boot-only checks.
        // Normal RouteTo during boot will not bypass these filters (see RouteTo clause below).
        if (m.force) {
          fx = fx + Effect.action {
            RouteTo( nextRoute2, force = m.force )
          }
        }

        //val v2 = (currRouteLens replace Some(nextRoute2))( v0 )
        //updatedSilent(v2, fx)
        effectOnly( fx )
      }

  }

}
