package io.suggest.sjs.leaflet.popup

import io.suggest.sjs.leaflet.map.Point

import scala.scalajs.js
import scala.scalajs.js.UndefOr

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 20.11.15 14:42
 * Description: API for popup options JSON.
 */

trait PopupOptions extends js.Object {

  val maxWidth: UndefOr[Int] = js.undefined

  val minWidth: UndefOr[Int] = js.undefined

  val maxHeight: UndefOr[Int] = js.undefined

  val autoPan: UndefOr[Boolean] = js.undefined

  val keepInView: UndefOr[Boolean] = js.undefined

  val closeButton: UndefOr[Boolean] = js.undefined

  val offset: UndefOr[Point] = js.undefined

  val autoPanPaddingTopLeft: UndefOr[Point] = js.undefined

  val autoPanPaddingBottomRight: UndefOr[Point] = js.undefined

  val autoPanPadding: UndefOr[Point] = js.undefined

  val zoomAnimation: UndefOr[Boolean] = js.undefined

  val closeOnClick: UndefOr[Boolean] = js.undefined

  val className: UndefOr[String] = js.undefined

}
