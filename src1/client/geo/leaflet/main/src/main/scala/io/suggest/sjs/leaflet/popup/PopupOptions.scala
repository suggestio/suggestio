package io.suggest.sjs.leaflet.popup

import io.suggest.sjs.common.model.FromDict
import io.suggest.sjs.leaflet.map.Point

import scala.scalajs.js
import scala.scalajs.js.UndefOr

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 20.11.15 14:42
 * Description: API for popup options JSON.
 */
object PopupOptions extends FromDict {
  override type T = PopupOptions
}


@js.native
trait PopupOptions extends js.Object {

  var maxWidth: Int = js.native

  var minWidth: Int = js.native

  var maxHeight: UndefOr[Int] = js.native

  var autoPan: Boolean = js.native

  var keepInView: Boolean = js.native

  var closeButton: Boolean = js.native

  var offset: Point = js.native

  var autoPanPaddingTopLeft: Point = js.native

  var autoPanPaddingBottomRight: Point = js.native

  var autoPanPadding: Point = js.native

  var zoomAnimation: Boolean = js.native

  var closeOnClick: UndefOr[Boolean] = js.native

  var className: String = js.native

}
