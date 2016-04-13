package io.suggest.sjs.mapbox.gl.layer

import io.suggest.sjs.common.model.FromDict
import io.suggest.sjs.mapbox.gl.Zoom_t

import scala.scalajs.js
import scala.scalajs.js.UndefOr
import scala.scalajs.js.annotation.JSName

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 12.04.16 13:57
  * Description: API for layer instances.
  */
object Layer extends FromDict {
  override type T = Layer
}


@js.native
trait Layer extends js.Object {

  var id: String = js.native

  var source: String = js.native

  @JSName("source-layer")
  var sourceLayer: String = js.native

  /** @see [[LayerTypes]]. */
  var `type`: UndefOr[String] = js.native

  var metadata: UndefOr[js.Dictionary[js.Any]] = js.native

  var ref: UndefOr[String] = js.native

  var minzoom: UndefOr[Zoom_t] = js.native

  var maxzoom: UndefOr[Zoom_t] = js.native

  var interactive: UndefOr[Boolean] = js.native

  var filter: UndefOr[js.Object] = js.native

  // https://www.mapbox.com/mapbox-gl-style-spec/#layers-background
  var layout: UndefOr[LayoutProps] = js.native

  var paint: UndefOr[PaintProps] = js.native

}
