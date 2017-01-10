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

}

/**
  * Реализация sjs-react-враппера для react-js-компонента [[MarkerClusterGroupC]].
  * sealed, чтобы можно было делать инстанс только через companion object, у которого есть очень нужный конструктор.
  */
sealed case class MarkerClusterGroupR(props: MarkerClusterGroupPropsR) extends JsWrapper0R[MarkerClusterGroupPropsR, TopNode] {
  override protected def _rawComponent = MarkerClusterGroupR.jsConstructor
}


/** Реализация компонента, овладевающего кластерами из маркеров. */
@ScalaJSDefined
class MarkerClusterGroupC(_props: MarkerClusterGroupPropsR, _ctx: Context)
  extends MapLayerR[MarkerClusterGroupPropsR](_props, _ctx)
{

  override type El_t = MarkerClusterGroup

  override def componentWillMount(): Unit = {
    val markers = props.markers
    val mcg = Leaflet.markerClusterGroup(props)
    mcg.addLayers( markers )
    // TODO Сделать обработчики событий аналогично react-leaflet? Все, начинающиеся с "on" подхватывать и навешивать?
    for (f <- props.markerClick) {
      mcg.on3(MarkerClusterEvents.CLICK, f)
    }
    for (f <- props.clusterClick) {
      mcg.on3( MarkerClusterEvents.CLUSTER_CLICK, f)
    }
    leafletElement = mcg
  }

  // TODO нужна реализация willUpdateProps(), сейчас пока компонент read-only как бы за ненадобностью read-write.

}


@ScalaJSDefined
trait MarkerClusterGroupPropsR extends MarkerClusterGroupOptions {

  val markers       : js.Array[Marker]

  val key           : UndefOr[String]                         = js.undefined

  val markerClick   : js.UndefOr[js.Function1[MarkerEvent,_]] = js.undefined

  val clusterClick  : js.UndefOr[js.Function1[MarkerEvent,_]] = js.undefined

}
