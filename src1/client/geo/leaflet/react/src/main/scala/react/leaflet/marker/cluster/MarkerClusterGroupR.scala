package react.leaflet.marker.cluster

import io.suggest.sjs.leaflet.marker.cluster._
import io.suggest.sjs.leaflet.marker.{Marker, MarkerEvent}
import japgolly.scalajs.react.{Children, JsComponent}
import react.leaflet.Context
import react.leaflet.layer.MapLayerR

import scala.scalajs.js
import scala.scalajs.js.UndefOr

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

  val component = JsComponent[MarkerClusterGroupPropsR, Children.None, Null]( jsConstructor )

  def apply(props: MarkerClusterGroupPropsR) = component( props )

  /** Выставить в cluster-группу новую инфу. */
  private[cluster] def _setLayers(props: MarkerClusterGroupPropsR, mcg: MarkerClusterGroup): Unit = {
    mcg.addLayers( props.markers )
    // TODO Сделать обработчики событий аналогично react-leaflet? Все, начинающиеся с "on" подхватывать и навешивать?
    for (f <- props.markerClick)
      mcg.on3(MarkerClusterEvents.CLICK, f)
    for (f <- props.clusterClick)
      mcg.on3( MarkerClusterEvents.CLUSTER_CLICK, f)
  }

}


/** Реализация компонента, овладевающего кластерами из маркеров. */
sealed class MarkerClusterGroupC(_props: MarkerClusterGroupPropsR, _ctx: Context)
  extends MapLayerR[MarkerClusterGroupPropsR](_props, _ctx)
{

  override type El_t = MarkerClusterGroup

  override def componentWillMount(): Unit = {
    val mcg = MarkerClusterGroup(props)
    MarkerClusterGroupR._setLayers(props, mcg)
    leafletElement = mcg
  }

  override def componentWillReceiveProps(nextProps: MarkerClusterGroupPropsR): Unit = {
    val mcg = leafletElement
    mcg.clearLayers()
    // Дропнуть все листенеры, т.к. в setLayers будут повешены новые листенеры.
    mcg.off()
    MarkerClusterGroupR._setLayers(nextProps, mcg)
  }

}


trait MarkerClusterGroupPropsR extends MarkerClusterGroupOptions {

  val markers       : js.Array[Marker]

  val key           : UndefOr[String]                               = js.undefined

  val markerClick   : js.UndefOr[js.Function1[MarkerEvent, Unit]]   = js.undefined

  val clusterClick  : js.UndefOr[js.Function1[MarkerEvent, Unit]]   = js.undefined

}
