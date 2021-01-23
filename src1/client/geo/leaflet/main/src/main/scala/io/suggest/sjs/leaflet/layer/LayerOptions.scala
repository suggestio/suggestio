package io.suggest.sjs.leaflet.layer

import scala.scalajs.js

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 23.01.2021 0:14
  */
trait LayerOptions extends js.Object {

  val pane: js.UndefOr[String] = js.undefined

  val attribution: js.UndefOr[String] = js.undefined

}
