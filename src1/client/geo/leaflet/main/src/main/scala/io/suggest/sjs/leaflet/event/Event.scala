package io.suggest.sjs.leaflet.event

import scala.scalajs.js

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 20.11.15 14:51
  * Description: API for abstract Leaflet Event.
  */
@js.native
trait Event extends js.Object {

  val `type`: String = js.native

  val target: js.Object = js.native

  val sourceTarget: js.UndefOr[js.Object] = js.native

  val propagatedFrom: js.UndefOr[js.Object] = js.native

}
