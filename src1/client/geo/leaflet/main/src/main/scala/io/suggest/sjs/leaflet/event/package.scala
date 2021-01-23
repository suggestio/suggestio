package io.suggest.sjs.leaflet

import scala.scalajs.js

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 22.01.2021 18:41
  */
package object event {

  type LeafletEventHandlerFn = js.Function1[Event, Unit]

  type LayersControlEventHandlerFn = js.Function1[LayersControlEvent, Unit];

  type LayerEventHandlerFn = js.Function1[LayerEvent, Unit]

  type ResizeEventHandlerFn = js.Function1[ResizeEvent, Unit]

  type PopupEventHandlerFn = js.Function1[PopupEvent, Unit]

  type TooltipEventHandlerFn = js.Function1[TooltipEvent, Unit]

  type ErrorEventHandlerFn = js.Function1[ErrorEvent, Unit]

  type LocationEventHandlerFn = js.Function1[LocationEvent, Unit]

  type LeafletMouseEventHandlerFn = js.Function1[MouseEvent, Unit]

  type LeafletKeyboardEventHandlerFn = js.Function1[KeyboardEvent, Unit]

  type ZoomAnimEventHandlerFn = js.Function1[ZoomAnimEvent, Unit]

  type DragEndEventHandlerFn = js.Function1[DragEndEvent, Unit]

  type TileEventHandlerFn = js.Function1[TileEvent, Unit]

  type TileErrorEventHandlerFn = js.Function1[TileErrorEvent, Unit]

}
