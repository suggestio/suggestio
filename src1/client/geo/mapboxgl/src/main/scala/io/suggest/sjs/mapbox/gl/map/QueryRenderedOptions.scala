package io.suggest.sjs.mapbox.gl.map

import io.suggest.sjs.common.model.FromDict

import scala.scalajs.js

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 12.04.16 14:45
  * Description:
  */
object QueryRenderedOptions extends FromDict {
  override type T = QueryRenderedOptions
}

@js.native
class QueryRenderedOptions extends js.Object {

  var layers: js.Array[String] = js.native

  var filter: js.Array[js.Any] = js.native

}
