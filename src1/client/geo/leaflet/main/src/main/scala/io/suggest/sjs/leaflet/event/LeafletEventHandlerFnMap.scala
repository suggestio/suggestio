package io.suggest.sjs.leaflet.event

import io.suggest.sjs.leaflet.marker.MarkerEvent

import scala.scalajs.js
import scala.scalajs.js.annotation.JSName

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 22.01.2021 23:49
  * @see [[https://github.com/DefinitelyTyped/DefinitelyTyped/blob/master/types/leaflet/index.d.ts#L259]]
  */
trait LeafletEventHandlerFnMap extends js.Object {

  val baselayerchange,
      overlayadd,
      overlayremove
      : js.UndefOr[LayersControlEventHandlerFn] = js.undefined

  val layeradd,
      layerremove
      : js.UndefOr[LayerEventHandlerFn] = js.undefined

  val zoomlevelschange,
      unload,
      viewreset,
      load,
      zoomstart,
      movestart,
      zoom,
      move,
      zoomend,
      moveend,
      autopanstart,
      dragstart,
      drag,
      add,
      remove,
      loading,
      error,
      update,
      down,
      animationend, spiderfied, unspiderfied,   // Leaflet.MarkerCluster
      predrag
      : js.UndefOr[LeafletEventHandlerFn] = js.undefined

  val resize: js.UndefOr[ResizeEventHandlerFn] = js.undefined

  val popupopen,
      popupclose
      : js.UndefOr[PopupEventHandlerFn] = js.undefined

  val tooltipopen,
      tooltipclose
      : js.UndefOr[TooltipEventHandlerFn] = js.undefined

  val locationerror: js.UndefOr[ErrorEventHandlerFn] = js.undefined

  val locationfound: js.UndefOr[LocationEventHandlerFn] = js.undefined

  val click,
      clusterclick,                               // Leaflet.MarkerCluster
      dblclick,
      mousedown,
      mouseup,
      mouseover,
      mouseout,
      mousemove,
      contextmenu,
      preclick
      : js.UndefOr[LeafletMouseEventHandlerFn] = js.undefined

  @JSName( "click" )
  val clusterMarkerClick: js.UndefOr[js.Function1[MarkerEvent, Unit]] = js.undefined  // Leaflet.MarkerCluster

  val keypress,
      keydown,
      keyup
      : js.UndefOr[LeafletKeyboardEventHandlerFn] = js.undefined

  val zoomanim: js.UndefOr[ZoomAnimEventHandlerFn] = js.undefined

  val dragend: js.UndefOr[DragEndEventHandlerFn] = js.undefined

  val tileunload,
      tileloadstart,
      tileload
      : js.UndefOr[TileEventHandlerFn] = js.undefined

  val tileerror: js.UndefOr[TileErrorEventHandlerFn] = js.undefined

}
