package react.leaflet.layer

import io.suggest.react.JsWrapper0R
import io.suggest.sjs.leaflet.tilelayer.TlOptions
import japgolly.scalajs.react.{JsComponentType, TopNode}

import scala.scalajs.js
import scala.scalajs.js.annotation.{JSImport, ScalaJSDefined}

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 08.12.16 17:19
  * Description: React-leaflet wrapper API for TileLayer component.
  */

case class TileLayerR(
  override val props: TileLayerPropsR
)
  extends JsWrapper0R[TileLayerPropsR, TopNode]
{
  override protected def _rawComponent = js.constructorOf[TileLayer]
}

@JSImport("react-leaflet", "TileLayer")
@js.native
sealed class TileLayer extends JsComponentType[TileLayerPropsR, js.Object, TopNode]


@ScalaJSDefined
trait TileLayerPropsR extends TlOptions {

  /** URL template for retriving tiles. */
  val url: String

}
