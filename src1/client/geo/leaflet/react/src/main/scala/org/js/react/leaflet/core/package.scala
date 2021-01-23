package org.js.react.leaflet

import japgolly.scalajs.react.raw

import scala.scalajs.js

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 22.01.2021 17:02
  */
package object core {

  final val PACKAGE_NAME = "@react-leaflet/core"

  /** @tparam E Leaflet element instance (e.g. Marker(), Control(), Layer(), etc ) */
  type ElementHookRef[E <: js.Object, C <: js.Any] = raw.React.RefHandle[LeafletElement[E, C]]

  type ElementHook[E <: js.Object, P <: js.Object] = js.Function2[P, LeafletContextInterface, ElementHookRef[E, js.Any]]

}
