package io.suggest.maps.r

import io.suggest.common.maps.leaflet.LeafletConstants
import io.suggest.msg.ErrorMsgs
import io.suggest.log.Log
import io.suggest.sjs.common.vm.wnd.WindowVm
import org.js.react.leaflet.{TileLayer, TileLayerProps}

import scala.scalajs.js

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 18.04.17 21:58
  * Description: Утиль для react + leaflet.
  */

object ReactLeafletUtil extends Log {

  /** Tile layer'ы. */
  object Tiles {

    /** Надо ли разрешать Leaflet'у детектить ретину?
      *
      * У него раньше были проблемы с этим, из-за чего появлялся слишком мелкий шрифт на тайлах
      * при низком device pixel ratio.
      *
      * @return true/false.
      */
    def isDetectRetina(): Boolean = {
      WindowVm().devicePixelRatio.fold {
        logger.warn( ErrorMsgs.SCREEN_PX_RATIO_MISSING )
        false
      } { pxRatio =>
        pxRatio >= 1.4
      }
    }

    def mkDefaultLayer() = {
      val _url = LeafletConstants.Tiles.URL_OSM_DFLT

      TileLayer.component.withKey( "tile:" + _url )(
        new TileLayerProps {
          override val url = _url
          override val detectRetina: js.UndefOr[Boolean] = {
            isDetectRetina()
          }
          override val attribution: js.UndefOr[String] = {
            LeafletConstants.Tiles.ATTRIBUTION_OSM
          }
        }
      )
    }

    /** Константный инстанс TileLayer компонента лежит в памяти отдельно, т.к. никаких изменений в нём не требуется. */
    lazy val OsmDefault = mkDefaultLayer()

  }


}
