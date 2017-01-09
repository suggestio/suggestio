package io.suggest.lk.adv.geo.m

import io.suggest.sjs.leaflet.marker.Marker

import scala.scalajs.js
import scala.scalajs.js.UndefOr

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 12.12.16 22:06
  * Description: Monkey-patching новым полем для макеров.
  */

@js.native
sealed trait MarkerNodeId extends js.Object {

  var nodeId: UndefOr[String] = js.native

}


object MarkerNodeId {

  import scala.language.implicitConversions

  implicit def apply(marker: Marker): MarkerNodeId = {
    marker.asInstanceOf[MarkerNodeId]
  }

}
