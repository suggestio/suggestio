package react.leaflet.gj

import io.suggest.sjs.leaflet.geojson.GeoJsonOptions
import japgolly.scalajs.react.{JsComponent, Children}

import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 08.12.16 19:02
  * Description: React API for GeoJSON leaflet layer component.
  */

object GeoJsonR {

  val component = JsComponent[GeoJsonPropsR, Children.None, Null]( LGeoJsonJs )

  def apply(props: GeoJsonPropsR) = component( props )

}


@JSImport("react-leaflet", "GeoJSON")
@js.native
object LGeoJsonJs extends js.Object


trait GeoJsonPropsR extends GeoJsonOptions {

  /** Обязательные данные для рендера. */
  val data: js.Any

}
