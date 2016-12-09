package react.leaflet.marker

import io.suggest.sjs.leaflet.Leaflet
import io.suggest.sjs.leaflet.marker.Marker
import io.suggest.sjs.leaflet.marker.cluster._
import japgolly.scalajs.react.TopNode
import react.leaflet.{Context, Wrapper0R}
import react.leaflet.layer.MapLayerR

import scala.scalajs.js
import scala.scalajs.js.UndefOr
import scala.scalajs.js.annotation.ScalaJSDefined

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 09.12.16 17:37
  * Description: React component implementation for basic support for Leaflet marker-cluster.
  */
object MarkerClusterGroupR {

  // Костыли для js static fields inheritance, которого нет в скале, но от них теперь зависит react:
  // see https://groups.google.com/d/msg/scala-js/v3gue_-Tms4/3M5cOSbACQAJ
  def jsConstructor = js.constructorOf[MarkerClusterGroup]
  jsConstructor.contextTypes = MapLayerR.contextTypes

  def apply( markers: js.Array[Marker],
             showCoverageOnHover: UndefOr[Boolean] = js.undefined,
             zoomToBoundsOnClick: UndefOr[Boolean] = js.undefined,
             spiderfyOnMaxZoom: UndefOr[Boolean]   = js.undefined,
             removeOutsideVisibleBounds: UndefOr[Boolean] = js.undefined,
             spiderLegPolylineOptions: UndefOr[js.Object] = js.undefined ): MarkerClusterGroupR = {

    val p = js.Dynamic.literal().asInstanceOf[MarkerClusterGroupPropsR]

    p.markers = markers
    showCoverageOnHover.foreach( p.showCoverageOnHover = _ )
    zoomToBoundsOnClick.foreach( p.zoomToBoundsOnClick = _ )
    spiderfyOnMaxZoom.foreach( p.spiderfyOnMaxZoom = _ )
    removeOutsideVisibleBounds.foreach( p.removeOutsideVisibleBounds = _ )
    spiderLegPolylineOptions.foreach( p.spiderLegPolylineOptions = _ )

    MarkerClusterGroupR(p)
  }

}


case class MarkerClusterGroupR(props: MarkerClusterGroupPropsR) extends Wrapper0R[MarkerClusterGroupPropsR, TopNode] {
  override protected def _rawComponent = MarkerClusterGroupR.jsConstructor
}

@ScalaJSDefined
class MarkerClusterGroup(props: MarkerClusterGroupPropsR, context: Context)
  extends MapLayerR[MarkerClusterGroupPropsR](props, context)
{

  override def componentWillMount(): Unit = {
    val mcg = Leaflet.markerClusterGroup(props)
    mcg.addLayers( props.markers )
    leafletElement = mcg
  }

}


@js.native
trait MarkerClusterGroupPropsR extends MarkerClusterGroupOptions {

  var markers: js.Array[Marker] = js.native

}
