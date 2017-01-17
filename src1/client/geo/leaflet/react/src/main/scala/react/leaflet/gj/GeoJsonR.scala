package react.leaflet.gj

import io.suggest.react.JsWrapperR
import io.suggest.sjs.leaflet.geojson.GjOptions
import japgolly.scalajs.react.JsComponentType
import org.scalajs.dom.raw.HTMLElement

import scala.scalajs.js
import scala.scalajs.js.annotation.{JSImport, ScalaJSDefined}

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 08.12.16 19:02
  * Description: React API for GeoJSON leaflet layer component.
  */

case class GeoJsonR(override val props: GeoJsonPropsR) extends JsWrapperR[GeoJsonPropsR, HTMLElement] {
  override protected def _rawComponent = js.constructorOf[GeoJSON]
}

@JSImport("react-leaflet", "GeoJSON")
@js.native
sealed class GeoJSON extends JsComponentType[GeoJsonPropsR, js.Object, HTMLElement]


@ScalaJSDefined
trait GeoJsonPropsR extends GjOptions {

  /** Обязательные данные для рендера. */
  val data: js.Any

}
