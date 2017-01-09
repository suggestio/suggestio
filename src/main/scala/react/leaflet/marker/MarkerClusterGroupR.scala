package react.leaflet.marker

import io.suggest.react.JsWrapper0R
import io.suggest.sjs.leaflet.Leaflet
import io.suggest.sjs.leaflet.marker.{Marker, MarkerEvent}
import io.suggest.sjs.leaflet.marker.cluster._
import japgolly.scalajs.react.TopNode
import react.leaflet.Context
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
  def jsConstructor = js.constructorOf[MarkerClusterGroupC]
  jsConstructor.contextTypes = MapLayerR.contextTypes

  def apply(markers: js.Array[Marker],
            onMarkerClick: js.UndefOr[MarkerEvent  => _] = js.undefined,
            onClusterClick: js.UndefOr[MarkerEvent => _] = js.undefined,
            key: UndefOr[String] = js.undefined,
            showCoverageOnHover: UndefOr[Boolean] = js.undefined,
            zoomToBoundsOnClick: UndefOr[Boolean] = js.undefined,
            spiderfyOnMaxZoom: UndefOr[Boolean]   = js.undefined,
            removeOutsideVisibleBounds: UndefOr[Boolean] = js.undefined,
            spiderLegPolylineOptions: UndefOr[js.Object] = js.undefined ): MarkerClusterGroupR = {

    val p = js.Dynamic.literal().asInstanceOf[MarkerClusterGroupPropsR]

    p.markers = markers
    onMarkerClick.foreach( f => p.markerClick = UndefOr.any2undefOrA(f) )
    onClusterClick.foreach( f => p.clusterClick = UndefOr.any2undefOrA(f) )
    key.foreach(p.key = _)
    showCoverageOnHover.foreach( p.showCoverageOnHover = _ )
    zoomToBoundsOnClick.foreach( p.zoomToBoundsOnClick = _ )
    spiderfyOnMaxZoom.foreach( p.spiderfyOnMaxZoom = _ )
    removeOutsideVisibleBounds.foreach( p.removeOutsideVisibleBounds = _ )
    spiderLegPolylineOptions.foreach( p.spiderLegPolylineOptions = _ )

    MarkerClusterGroupR(p)
  }

}


case class MarkerClusterGroupR(props: MarkerClusterGroupPropsR) extends JsWrapper0R[MarkerClusterGroupPropsR, TopNode] {
  override protected def _rawComponent = MarkerClusterGroupR.jsConstructor
}

@ScalaJSDefined
class MarkerClusterGroupC(_props: MarkerClusterGroupPropsR, _ctx: Context)
  extends MapLayerR[MarkerClusterGroupPropsR](_props, _ctx)
{

  override type El_t = MarkerClusterGroup

  override def componentWillMount(): Unit = {
    val markers = props.markers
    val mcg = Leaflet.markerClusterGroup(props)
    mcg.addLayers( markers )
    props.markerClick.foreach { f =>
      mcg.on3(MarkerClusterEvents.CLICK, f)
    }
    props.clusterClick.foreach { f =>
      mcg.on3( MarkerClusterEvents.CLUSTER_CLICK, f )
    }
    leafletElement = mcg
  }

  // TODO нужна реализация willUpdateProps(), сейчас пока компонент read-only как бы за ненадобностью read-write.

}


@js.native
trait MarkerClusterGroupPropsR extends MarkerClusterGroupOptions {

  var markers: js.Array[Marker] = js.native

  var key: String = js.native

  var markerClick: js.UndefOr[js.Function1[MarkerEvent,_]] = js.native

  var clusterClick: js.UndefOr[js.Function1[MarkerEvent,_]] = js.native

}
