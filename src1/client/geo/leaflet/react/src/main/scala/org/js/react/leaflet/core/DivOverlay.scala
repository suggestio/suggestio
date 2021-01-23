package org.js.react.leaflet.core

import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 24.01.2021 0:07
  */
@js.native
@JSImport(PACKAGE_NAME, "createDivOverlayHook")
object createDivOverlayHook extends js.Function {
  def apply[E <: DivOverlay, P <: LayerProps]
           (useElement: ElementHook[E, P], useLifecycle: DivOverlayLifecycleHook[E, P])
           : js.Function2[P, SetOpenFunc, ElementHookRef[E, js.Any]]
           = js.native
}
