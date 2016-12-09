package react.leaflet.lmap

import japgolly.scalajs.react.{JsComponentType, ReactElement}
import org.scalajs.dom.raw.HTMLElement
import react.leaflet.Context

import scala.scalajs.js
import scala.scalajs.js.annotation.JSName

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 09.12.16 13:37
  * Description: Facade API for ReactLeaflet.MapComponent.
  */
@js.native
@JSName("ReactLeaflet.MapComponent")
class MapComponentR[Props <: js.Any](
  val props: Props,
  val context: Context
)
  extends JsComponentType[Props, js.Object, HTMLElement]
{

  protected var leafletElement: js.Object = js.native

  def componentWillMount(): Unit = js.native

  def componentDidMount(): Unit = js.native


  def componentDidUpdate(prevProps: Props): Unit = js.native

  def componentWillUnmount(): Unit = js.native

  def componentDidUnmount(): Unit = js.native


  protected var _leafletEvents: js.Dictionary[js.Function] = js.native

  protected def extractLeafletEvents(props: Props): js.Dictionary[js.Function] = js.native

  protected def bindLeafletEvents(next: js.Object = js.native, prev: js.Object = js.native): js.Object = js.native

  def fireLeafletEvent(etype: String, data: js.Any = js.native): Unit = js.native

  /** @return ~Props with or without 'pane' field. So it can be safely casted to Props type. */
  def getOptions(props: Props): js.Dictionary[js.Any] = js.native

}

