package io.suggest.sjs.mapbox.gl.layer

import io.suggest.sjs.mapbox.gl.{Filter_t, Zoom_t}

import scala.scalajs.js
import scala.scalajs.js.UndefOr

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 12.04.16 13:57
  * Description: API for layer instances.
  */

trait Layer extends js.Object {

  val id: String

  /** @see [[LayerTypes]]. */
  val `type`: String

  val source: String

  val `source-layer`: UndefOr[String] = js.undefined

  val metadata: UndefOr[js.Dictionary[js.Any]] = js.undefined

  val ref: UndefOr[String] = js.undefined

  val minzoom: UndefOr[Zoom_t] = js.undefined

  val maxzoom: UndefOr[Zoom_t] = js.undefined

  val interactive: UndefOr[Boolean] = js.undefined

  /**
    * @see [[Filters]]
    * @see [[Clusters]]
    */
  val filter: UndefOr[Filter_t] = js.undefined

  // https://www.mapbox.com/mapbox-gl-style-spec/#layers-background
  val layout: UndefOr[LayoutProps] = js.undefined

  val paint: UndefOr[PaintProps] = js.undefined

}
