package org.js.react.leaflet.core

import io.suggest.sjs.leaflet.layer.group.LayerGroup
import io.suggest.sjs.leaflet.map.{LMap, Layer}
import japgolly.scalajs.react.raw

import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport
import scala.scalajs.js.|

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 22.01.2021 16:55
  */

trait ControlledLayer extends js.Object {
  def addLayer(layer: Layer): Unit
  def removeLayer(layer: Layer): Unit
}


trait LeafletContextInterface extends js.Object {
  val __version: Double
  val map: LMap
  val layerContainer: js.UndefOr[ControlledLayer | LayerGroup] = js.undefined
  val layersControl: js.UndefOr[/*TODO LControl.Layers*/ js.Any] = js.undefined
  val overlayContainer: js.UndefOr[Layer] = js.undefined
  val pane: js.UndefOr[String] = js.undefined
}


@js.native
@JSImport(PACKAGE_NAME, "LeafletContext")
object LeafletContext extends raw.React.Context[LeafletContextInterface]


@js.native
@JSImport(PACKAGE_NAME, "useLeafletContext")
object useLeafletContext extends js.Function0[LeafletContextInterface] {
  override def apply(): LeafletContextInterface = js.native
}
