package org.js.react.leaflet

import io.suggest.sjs.leaflet.marker.Marker
import io.suggest.sjs.leaflet.marker.cluster.{MarkerClusterGroupOptions, MarkerClusterGroup => LeafletMarkerClusterGroup}
import japgolly.scalajs.react.{Children, JsForwardRefComponent}
import org.js.react.leaflet.core.{EventedProps, LayerProps, LeafletContextInterface, LeafletElement, createLayerComponent, useEventHandlers}

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

      for (fnMap <- props.eventHandlers)
        useEventHandlers( leafEl, fnMap )

      leafEl
    },

    updateElement = { (mcg, props0, props2) =>
      if (props0.markers ne props2.markers) {
        mcg.clearLayers()
           .addLayers( props2.markers )
      }

      for {
        fnMap <- props2.eventHandlers
        if !(props0.eventHandlers contains fnMap)
      } {
        val leafEl = new LeafletElement[LeafletMarkerClusterGroup, js.Any] {
          override val instance = mcg
          // TODO Внутри useEventHandlers() оно не испольузется, но это ненормально - пропихивать null наружу.
          override val context = null
        }
        useEventHandlers(leafEl, fnMap)
      }
    },

  )

  val component = JsForwardRefComponent[MarkerClusterGroupProps, Children.None, LeafletMarkerClusterGroup]( componentRaw )

}


trait MarkerClusterGroupProps extends MarkerClusterGroupOptions with LayerProps with EventedProps {
  val markers         : js.Array[Marker]
}
