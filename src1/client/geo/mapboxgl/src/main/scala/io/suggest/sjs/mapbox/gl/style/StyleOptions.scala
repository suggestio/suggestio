package io.suggest.sjs.mapbox.gl.style

import io.suggest.sjs.common.model.FromDict

import scala.scalajs.js

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 12.04.16 11:56
  * Description: API for style options json.
  */
object StyleOptions extends FromDict {
  override type T = StyleOptions
}


@js.native
sealed trait StyleOptions extends js.Object {

  var transition: Boolean = js.native

}
