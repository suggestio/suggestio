package react.leaflet.layer

import io.suggest.sjs.leaflet.tilelayer.TlOptions
import japgolly.scalajs.react.TopNode
import react.leaflet.Wrapper0R

import scala.scalajs.js
import scala.scalajs.js.UndefOr

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 08.12.16 17:19
  * Description: React-leaflet wrapper API for TileLayer component.
  */
object TileLayerR {

  def apply(
    url           : String,
    attribution   : UndefOr[String]  = js.undefined,
    detectRetina  : UndefOr[Boolean] = js.undefined,
    opacity       : UndefOr[Double]  = js.undefined,
    zIndex        : UndefOr[Int]     = js.undefined
    // TODO Grab more options from TlOptions trait.
  ): TileLayerR = {

    val p = js.Dynamic.literal().asInstanceOf[TileLayerPropsR]

    p.url = url
    attribution.foreach(p.attribution = _)
    detectRetina.foreach(p.detectRetina = _)
    opacity.foreach(p.opacity = _)
    zIndex.foreach(p.zIndex = _)

    TileLayerR(p)
  }

}


case class TileLayerR(
  override val props: TileLayerPropsR
)
  extends Wrapper0R[TileLayerPropsR, TopNode]
{
  override protected def _rawComponent = js.Dynamic.global.ReactLeaflet.TileLayer
}


@js.native
trait TileLayerPropsR extends TlOptions {

  var url         : String           = js.native

}
