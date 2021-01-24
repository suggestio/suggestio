package org.js.react.leaflet.core

import japgolly.scalajs.react.raw
import japgolly.scalajs.react.raw.PropsWithChildren

import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 24.01.2021 0:37
  */
@js.native
@JSImport(PACKAGE_NAME, "createContainerComponent")
object createContainerComponent extends js.Function {
  def apply[E, P <: PropsWithChildren]
           (useElement: ElementHook[E, P])
           : raw.React.ForwardRefComponent[P, E]
           = js.native
}


@js.native
@JSImport(PACKAGE_NAME, "createDivOverlayComponent")
object createDivOverlayComponent extends js.Function {
  def apply[E <: DivOverlay, P <: PropsWithChildren]
           (useElement: DivOverlayHookRes[E, P])
           : raw.React.ForwardRefComponent[P, E]
           = js.native
}


@js.native
@JSImport(PACKAGE_NAME, "createLeafComponent")
object createLeafComponent extends js.Function {
  def apply[E, P <: js.Object]
           (useElement: ElementHook[E, P])
           : raw.React.ForwardRefComponent[P, E]
           = js.native
}
