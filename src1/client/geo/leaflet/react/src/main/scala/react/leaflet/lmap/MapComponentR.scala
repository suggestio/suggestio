package react.leaflet.lmap

import react.leaflet.Context

import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 09.12.16 13:37
  * Description: Facade API for ReactLeaflet.MapComponent.
  */
@JSImport("react-leaflet", "MapComponent")
@js.native
class MapComponentR[Props <: js.Any](
  val props: Props,
  val context: Context
)
  extends js.Object
{

  type El_t <: js.Object

  protected var leafletElement: El_t = js.native

  def componentWillMount(): Unit = js.native

  def componentDidMount(): Unit = js.native


  def componentDidUpdate(prevProps: Props): Unit = js.native

  def componentWillUnmount(): Unit = js.native


  protected var _leafletEvents: js.Dictionary[js.Function] = js.native

  protected def extractLeafletEvents(props: Props): js.Dictionary[js.Function] = js.native

  protected def bindLeafletEvents(next: js.Dictionary[js.Function] = js.native, prev: js.Dictionary[js.Function] = js.native): js.Object = js.native

  def fireLeafletEvent(etype: String, data: js.Any = js.native): Unit = js.native

  /** @return ~Props with or without 'pane' field. So it can be safely casted to Props type. */
  def getOptions(props: Props): js.Dictionary[js.Any] = js.native

}

