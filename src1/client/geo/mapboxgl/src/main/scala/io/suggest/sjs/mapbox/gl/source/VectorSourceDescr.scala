package io.suggest.sjs.mapbox.gl.source

import io.suggest.sjs.common.model.FromDict

import scala.scalajs.js
import scala.scalajs.js.UndefOr

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 13.04.16 22:16
  * Description: Descriptor for vector sources.
  */
object VectorSourceDescr extends FromDict {
  override type T = VectorSourceDescr
}


@js.native
class VectorSourceDescr extends SourceDescr {

  var url: UndefOr[String] = js.native

  var tiles: UndefOr[js.Array[String]] = js.native

  var tileSize: UndefOr[Int] = js.native

}
