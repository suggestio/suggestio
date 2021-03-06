package org.js.react.leaflet.core

import io.suggest.sjs.leaflet.event.{Evented, LeafletEventHandlerFnMap}

import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 22.01.2021 23:59
  */
trait EventedProps extends js.Object {
  val eventHandlers: js.UndefOr[LeafletEventHandlerFnMap] = js.undefined
}


@js.native
@JSImport(PACKAGE_NAME, "useEventHandlers")
object useEventHandlers extends js.Function {
  def apply[E <: Evented, C <: js.Any]
           (element: LeafletElement[E, C],
            eventHandlers: LeafletEventHandlerFnMap = js.native)
           : Unit = js.native
}
