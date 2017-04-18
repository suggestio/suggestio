package io.suggest.maps.r

import io.suggest.common.maps.leaflet.LeafletConstants
import io.suggest.sjs.common.log.Log
import io.suggest.sjs.common.msg.WarnMsgs
import io.suggest.sjs.common.vm.wnd.WindowVm
import react.leaflet.layer.{TileLayerPropsR, TileLayerR}

import scala.scalajs.js.UndefOr

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 18.04.17 21:58
  * Description: Утиль для react + leaflet.
  */
object ReactLeafletUtil extends Log {

  /** Tile layer'ы. */
  object Tiles {

    /** Константный инстанс TileLayer компонента лежит в памяти отдельно, т.к. никаких изменений в нём не требуется. */
    lazy val OsmDefault = {
      TileLayerR(
        new TileLayerPropsR {
          override val url           = LeafletConstants.Tiles.URL_OSM_DFLT

          override val detectRetina: UndefOr[Boolean] = {
            WindowVm().devicePixelRatio.fold {
              LOG.warn( WarnMsgs.SCREEN_PX_RATIO_MISSING )
              false
            } { pxRatio =>
              pxRatio >= 1.4
            }
          }

          override val attribution   = LeafletConstants.Tiles.ATTRIBUTION_OSM
        }
      )()
    }

  }


}
