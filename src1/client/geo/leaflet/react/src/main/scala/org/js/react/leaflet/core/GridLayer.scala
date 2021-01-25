package org.js.react.leaflet.core

import io.suggest.sjs.leaflet.layer.grid.{GridLayer, GridLayerOptions}

import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 25.01.2021 17:12
  * Description:
  */
@js.native
@JSImport(PACKAGE_NAME, "updateGridLayer")
object updateGridLayer extends js.Function {
  def apply[E <: GridLayer, P <: GridLayerOptions]
           (layer: E, props: P, prevProps: P)
           : Unit = js.native
}
