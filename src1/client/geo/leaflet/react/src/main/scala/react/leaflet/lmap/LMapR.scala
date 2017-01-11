package react.leaflet.lmap

import io.suggest.react.JsWrapperR
import io.suggest.sjs.leaflet.event.{LocationEvent, PopupEvent}
import io.suggest.sjs.leaflet.map.{LatLngBounds, MapOptions}
import org.scalajs.dom.raw.HTMLDivElement

import scala.scalajs.js
import scala.scalajs.js.UndefOr
import scala.scalajs.js.annotation.ScalaJSDefined

/**
  * Suggest.io
  * User: UndefOr[Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: UndefOr[08.12.16 12:20
  * Description: UndefOr[React leaflet map wrapper по упрощенным технологиям.
  * @see [[https://github.com/chandu0101/scalajs-react-components/blob/master/doc/InteropWithThirdParty.md]]
  */

case class LMapR(
  override val props: LMapPropsR
)
  extends JsWrapperR[LMapPropsR, HTMLDivElement]
{

  override protected def _rawComponent = js.Dynamic.global.ReactLeaflet.Map

}


@ScalaJSDefined
trait LMapPropsR extends MapOptions {

  val animate         : UndefOr[Boolean]        = js.undefined
  val bounds          : UndefOr[LatLngBounds]   = js.undefined
  val boundsUndefOrs  : UndefOr[js.Object]      = js.undefined
  val className       : UndefOr[String]         = js.undefined
  val style           : UndefOr[String]         = js.undefined
  val id              : UndefOr[String]         = js.undefined
  val useFlyTo        : UndefOr[Boolean]        = js.undefined

  /**
    * Optional reaction about detected location (L.control.locate).
    * Handled automatically inside MapComponent.bindLeafletEvents().
    */
  val onLocationFound: UndefOr[js.Function1[LocationEvent, Unit]] = js.undefined

  val onPopupClose: UndefOr[js.Function1[PopupEvent, Unit]] = js.undefined

}
