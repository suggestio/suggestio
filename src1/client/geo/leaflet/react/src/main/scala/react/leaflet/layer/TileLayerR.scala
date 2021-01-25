package react.leaflet.layer

import io.suggest.sjs.leaflet.layer.tile.TileLayerOptions
import japgolly.scalajs.react.{Children, JsComponent}

import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 08.12.16 17:19
  * Description: React-leaflet wrapper API for TileLayer component.
  */

object TileLayerR {

  val component = JsComponent[TileLayerPropsR, Children.None, Null]( TileLayerJsR )

  def apply(props: TileLayerPropsR) = component(props)

}


@JSImport("react-leaflet", "TileLayer")
@js.native
object TileLayerJsR extends js.Object // JsComponentType[TileLayerPropsR, js.Object, TopNode]


trait TileLayerPropsR extends TileLayerOptions {

  /** URL template for retriving tiles. */
  val url: String

}
