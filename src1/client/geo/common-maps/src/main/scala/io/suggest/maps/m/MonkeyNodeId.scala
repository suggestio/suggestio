package io.suggest.maps.m

import scala.scalajs.js

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 12.12.16 22:06
  * Description: Monkey-patching новым полем для макеров.
  */

@js.native
sealed trait MonkeyNodeId extends js.Object {

  var nodeId: js.UndefOr[String] = js.native

}


object MonkeyNodeId {

  import scala.language.implicitConversions

  implicit def forJsObject(marker: js.Object): MonkeyNodeId = {
    marker.asInstanceOf[MonkeyNodeId]
  }

}
