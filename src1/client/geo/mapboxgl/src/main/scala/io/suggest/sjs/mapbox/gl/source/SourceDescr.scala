package io.suggest.sjs.mapbox.gl.source

import io.suggest.sjs.common.model.FromDict
import io.suggest.sjs.mapbox.gl.Zoom_t

import scala.scalajs.js
import scala.scalajs.js.UndefOr

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 12.04.16 14:06
  * Description: Source descriptors API.
  */
object SourceDescr extends FromDict {
  override type T = SourceDescr
}


@js.native
trait SourceDescr extends js.Object {

  var `type`: String = js.native
  var maxzoom: UndefOr[Zoom_t] = js.native

  //var urls: UndefOr[js.Array[String]] = js.native
  //var coordinates: UndefOr[js.Array[js.Array[Double]]] = js.native

}
