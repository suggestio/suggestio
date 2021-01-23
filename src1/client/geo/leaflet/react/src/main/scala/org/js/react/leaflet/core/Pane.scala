package org.js.react.leaflet.core

import io.suggest.sjs.leaflet.layer.LayerOptions

import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 23.01.2021 0:12
  */
@js.native
@JSImport(PACKAGE_NAME, "withPane")
object withPane extends js.Function {
  def apply[P <: LayerOptions](props: P,
                               context: LeafletContextInterface,
                              ): P = js.native
}
