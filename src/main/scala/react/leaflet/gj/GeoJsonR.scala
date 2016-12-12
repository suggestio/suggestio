package react.leaflet.gj

import io.suggest.react.JsWrapperR
import io.suggest.sjs.common.geo.json.GjFeature
import io.suggest.sjs.leaflet.geojson.{GjFeatureStyle, GjOptions}
import io.suggest.sjs.leaflet.map.{Layer, LatLng}
import org.scalajs.dom.raw.HTMLElement

import scala.scalajs.js
import scala.scalajs.js.UndefOr

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 08.12.16 19:02
  * Description: React API for GeoJSON leaflet layer component.
  */
object GeoJsonR {

  // TODO implement args from GjOptions.
  def apply(
             data           : js.Any,
             pointToLayer   : UndefOr[(GjFeature, LatLng) => Layer]  = js.undefined,
             onEachFeature  : UndefOr[(GjFeature, Layer) => Unit]    = js.undefined,
             style          : UndefOr[GjFeature => GjFeatureStyle]    = js.undefined,
             filter         : UndefOr[(GjFeature, Layer) => Boolean] = js.undefined,
             coordsToLatLng : UndefOr[js.Array[Double] => LatLng]     = js.undefined
           ): GeoJsonR = {

    val p = js.Dynamic.literal().asInstanceOf[GeoJsonPropsR]

    p.data = data
    pointToLayer.foreach(p.pointToLayer = _)
    onEachFeature.foreach(p.onEachFeature = _)
    style.foreach(p.style = _)
    filter.foreach(p.filter = _)
    coordsToLatLng.foreach(p.coordsToLatLng = _)

    GeoJsonR(p)
  }

}


case class GeoJsonR(override val props: GeoJsonPropsR) extends JsWrapperR[GeoJsonPropsR, HTMLElement] {
  override protected def _rawComponent = js.Dynamic.global.ReactLeaflet.GeoJSON
}


@js.native
trait GeoJsonPropsR extends GjOptions {

  var data: js.Any = js.native

}
