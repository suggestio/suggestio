package io.suggest.sc.util

import io.suggest.common.empty.OptionUtil
import io.suggest.sc.model.MScRoot
import io.suggest.sc.model.in.{MInternalInfo, MScInternals}
import io.suggest.sc.model.inx.MScIndex
import io.suggest.sc.model.search.{MGeoTabS, MScSearch}
import io.suggest.spa.SioPages

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 25.07.17 12:53
  * Description: Outer SPA URL-router interaction controller.
  */
object ScRoutingUtil {

  /** Make current state data for URL.
    *
    * @param v0 MScRoot instance.
    * @return Sc3 state URL snapshot.
    */
  def getMainScreenSnapShot(v0: MScRoot): SioPages.Sc3 = {
    val inxState = v0.index.state
    val searchOpened = v0.index.search.panel.opened

    // If nothing in view (showcase not yet fully loaded), do not mess original values.
    val bootingRoute = v0.internals.info.currRoute
      .filter(_ => inxState.viewCurrent.isEmpty && v0.index.resp.isEmpty)
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
      nodeId = currRcvrId,
      locEnv = locEnv2,
      generation = Some(inxState.generation),
      searchOpened = searchOpened,
      tagNodeId = selTagIdOpt,
      menuOpened = v0.index.menu.opened,
      focusedAdId = for {
        scAdLoc <- v0.grid.core.ads.interactAdOpt
        scAd = scAdLoc.getLabel
        adData <- scAd.data.toOption
        if adData.isOpened
        nodeId <- adData.doc.tagId.nodeId
      } yield {
        nodeId
      },
      firstRunOpen = v0.dialogs.first.view.nonEmpty,
      dlAppOpen = v0.index.menu.dlApp.opened,
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
      .andThen(MScSearch.geo)
      .andThen(MGeoTabS.mapInit)
  }


  def root_internals_info_currRoute_LENS = {
    MScRoot.internals
      .andThen(MScInternals.info)
      .andThen(MInternalInfo.currRoute)
  }

}
