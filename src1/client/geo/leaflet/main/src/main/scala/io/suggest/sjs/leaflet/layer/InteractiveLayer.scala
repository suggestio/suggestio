package io.suggest.sjs.leaflet.layer

import io.suggest.sjs.leaflet.map.Layer

import scala.scalajs.js

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 23.01.2021 0:16
  * @see [[https://leafletjs.com/reference-1.6.0.html#interactive-layer]]
  */
trait InteractiveLayer extends Layer

trait InteractiveLayerOptions extends LayerOptions {

  val interactive,
      bubblingMouseEvents: js.UndefOr[Boolean] = js.undefined

}
