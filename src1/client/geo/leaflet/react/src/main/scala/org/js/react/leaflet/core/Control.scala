package org.js.react.leaflet.core

import io.suggest.sjs.leaflet.control.{Control, ControlOptions}

import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 24.01.2021 23:17
  */

@js.native
@JSImport(PACKAGE_NAME, "createControlHook")
object createControlHook extends js.Function {
  def apply[E <: Control, P <: ControlOptions]
           (useElement: ElementHook[E, P])
           : js.Function1[P, ElementHookRef[E, js.Any]]
           = js.native
}
