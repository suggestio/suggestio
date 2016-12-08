package react.leaflet.marker

import io.suggest.sjs.leaflet.marker.icon.Icon
import org.scalajs.dom.raw.HTMLElement
import react.leaflet.WrapperR

import scala.scalajs.js
import scala.scalajs.js.UndefOr

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 08.12.16 16:35
  * Description: React wrapper for react-leaflet Marker component.
  */

object MarkerR {

  def apply(
    icon          : UndefOr[Icon] = js.undefined,
    opacity       : UndefOr[Int]  = js.undefined,
    zIndexOffset  : UndefOr[Int]  = js.undefined
  ): MarkerR = {

    val p = js.Dynamic.literal().asInstanceOf[MarkerPropsR]
    icon.foreach( p.icon = _ )
    opacity.foreach( p.opacity = _ )
    zIndexOffset.foreach( p.zIndexOffset = _ )

    MarkerR(p)
  }

}


case class MarkerR(
  override val props: MarkerPropsR
)
  extends WrapperR[MarkerPropsR, HTMLElement] {

  override protected def _rawComponent = js.Dynamic.global.ReactLeaflet.Marker

}


@js.native
trait MarkerPropsR extends js.Object {

  var icon          : UndefOr[Icon] = js.native
  var opacity       : UndefOr[Int]  = js.native
  var zIndexOffset  : UndefOr[Int]  = js.native

}

