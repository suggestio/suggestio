package org.js.react.leaflet

import io.suggest.sjs.leaflet.event.LeafletEventHandlerFnMap
import io.suggest.sjs.leaflet.marker.Marker
import io.suggest.sjs.leaflet.marker.cluster.{MarkerClusterEvents, MarkerClusterGroupOptions, MarkerClusterGroup => LeafletMarkerClusterGroup}
import japgolly.scalajs.react.{Children, JsForwardRefComponent}
import org.js.react.leaflet.core.{EventedProps, LayerProps, LeafletContextInterface, LeafletElement, createLayerComponent}
import io.suggest.ueq.JsUnivEqUtil._
import io.suggest.ueq.UnivEqUtil._

import scala.scalajs.js

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 26.01.2021 18:40
  * Description: MarkerClusterGroup impl. for react-leaflet v3.
  */
object MarkerClusterGroup {

  val componentRaw = createLayerComponent[LeafletMarkerClusterGroup, MarkerClusterGroupProps](

    createElement = { (props, context0) =>
      val mcg = LeafletMarkerClusterGroup( props )
      mcg.addLayers( props.markers )

      val layCont2 = context0.layerContainer
        .getOrElse( context0.map )

      val context2 = new LeafletContextInterface {
        override val __version = context0.__version
        override val map = context0.map
        override val layerContainer = layCont2
        override val layersControl = context0.layersControl
        override val overlayContainer = mcg
        override val pane = context0.pane
      }

      val leafEl = new LeafletElement[LeafletMarkerClusterGroup, js.Any] {
        override val instance = mcg
        override val context = context2
      }

      for (fnMap <- props.eventHandlers) {
        for (onClusterMarkerClickF <- fnMap.clusterMarkerClick)
          mcg.on3( MarkerClusterEvents.CLICK, onClusterMarkerClickF )
        for (onClusterClick <- fnMap.clusterclick)
          mcg.on3( MarkerClusterEvents.CLUSTER_CLICK, onClusterClick )

        // НЕЛЬЗЯ использовать useEventHandlers(), т.к. effect будет сброшен до первого update, а внутри update хуки не пашут, и эффект повторно организовать уже нельзя.
        //useEventHandlers( leafEl, fnMap )
      }

      leafEl
    },

    updateElement = { (mcg, props2, props0) =>
      if (props0.markers ne props2.markers) {
        mcg.clearLayers()
           .addLayers( props2.markers )
      }

      for {
        fnMap2 <- props2.eventHandlers
        if !(props0.eventHandlers contains fnMap2)
      } {
        def _updateCallback[T](eventType: String, getFn: LeafletEventHandlerFnMap => js.UndefOr[js.Function1[T, Unit]]): Unit = {
          val fn0U = props0.eventHandlers
            .flatMap(getFn)
          val fn2U = getFn( fnMap2 )
          if (fn2U !===* fn0U) {
            for (fn0 <- fn0U)
              mcg.off3( eventType, fn0 )
            for (fn2 <- fn2U)
              mcg.on3( eventType, fn2 )
          }
        }
        _updateCallback( MarkerClusterEvents.CLICK, _.clusterMarkerClick )
        _updateCallback( MarkerClusterEvents.CLUSTER_CLICK, _.clusterclick )
      }
    },

  )

  val component = JsForwardRefComponent[MarkerClusterGroupProps, Children.None, LeafletMarkerClusterGroup]( componentRaw )

}


trait MarkerClusterGroupProps extends MarkerClusterGroupOptions with LayerProps with EventedProps {
  val markers         : js.Array[Marker]
}
