package org.js.react.leaflet

import io.suggest.sjs.leaflet.popup.Popup
import io.suggest.sjs.leaflet.tooltip.Tooltip
import japgolly.scalajs.react.raw

import scala.scalajs.js
import scala.scalajs.js.|

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 22.01.2021 17:02
  */
package object core {

  final val PACKAGE_NAME = "@react-leaflet/core"

  /** @tparam E Leaflet element instance (e.g. Marker(), Control(), Layer(), etc ).
    *           Без ограничений, т.к. DivOverlay - это scala.Any.
    */
  type ElementHookRef[E, C <: js.Any] = raw.React.RefHandle[LeafletElement[E, C]]

  type ElementHook[E, P <: js.Object] = js.Function2[P, LeafletContextInterface, ElementHookRef[E, js.Any]]

  type DivOverlay = Popup | Tooltip
  type SetOpenFunc = js.Function1[Boolean, Unit]

  type DivOverlayLifecycleHook[E, P <: js.Object] =
    js.Function4[LeafletElement[E, js.Any], LeafletContextInterface, P, SetOpenFunc, Unit]

  type DivOverlayHookRes[E <: DivOverlay, P <: js.Object] =
    js.Function2[P, SetOpenFunc, ElementHookRef[E, js.Any]]
  type DivOverlayHook[E <: DivOverlay, P <: js.Object] =
    js.Function2[ElementHook[E, P], DivOverlayLifecycleHook[E, P], DivOverlayHookRes[E, P]]

}
