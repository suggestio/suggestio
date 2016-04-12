package io.suggest.sjs.mapbox.gl.style.source

import io.suggest.sjs.common.model.FromDict
import io.suggest.sjs.mapbox.gl.Zoom_t

import scala.scalajs.js
import scala.scalajs.js.UndefOr

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 12.04.16 14:06
  * Description: Style.source model API.
  */
object Source extends FromDict {
  override type T = Source
}


@js.native
trait Source extends js.Object {

  var `type`: String = js.native

  var url: UndefOr[String] = js.native

  var urls: UndefOr[js.Array[String]] = js.native

  var maxzoom: UndefOr[Zoom_t] = js.native

  var tiles: UndefOr[js.Array[String]] = js.native

  var tileSize: UndefOr[Int] = js.native

  var data: UndefOr[js.Object] = js.native

  var coordinates: UndefOr[js.Array[js.Array[Double]]] = js.native

}
