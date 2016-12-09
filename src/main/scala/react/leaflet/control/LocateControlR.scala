package react.leaflet.control

import io.suggest.sjs.leaflet.Leaflet
import io.suggest.sjs.leaflet.control.locate.LocateOptions
import io.suggest.sjs.leaflet.event.LocationEvent
import japgolly.scalajs.react.ReactElement
import org.scalajs.dom.raw.HTMLElement
import react.leaflet.lmap.MapComponentR
import react.leaflet.{Context, Wrapper0R}

import scala.scalajs.js
import scala.scalajs.js.UndefOr
import scala.scalajs.js.annotation.ScalaJSDefined

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 09.12.16 12:57
  * Description: Implementation for react-component for L.control.locate plugin.
  */

object LocateControlR {

  // Костыли для js static fields inheritance, которого нет в скале, но от них теперь зависит react:
  // see https://groups.google.com/d/msg/scala-js/v3gue_-Tms4/3M5cOSbACQAJ
  def jsConstructor = js.constructorOf[LocateControl]
  jsConstructor.contextTypes = MapControlR.contextTypes

  def apply(
           onLocationFound: UndefOr[LocationEvent => _] = js.undefined
           ): LocateControlR = {
    val p = js.Dynamic.literal().asInstanceOf[LocateControlPropsR]
    onLocationFound.foreach(p.onlocationfound = _)

    LocateControlR(p)
  }

}


/** Scala.js-враппер для js-класса [[LocateControl]]. */
case class LocateControlR(
  override val props: LocateControlPropsR
)
  extends Wrapper0R[LocateOptions, HTMLElement]
{
  override protected def _rawComponent = LocateControlR.jsConstructor
}


/** Рабочий (благодаря jsConstructor) прототип фасада ES6-класса LocateControl. */
@ScalaJSDefined
sealed class LocateControl(props: LocateOptions, context: Context)
  extends MapComponentR[LocateOptions](props, context)
{

  override def componentWillMount(): Unit = {
    leafletElement = Leaflet.control.locate( props )
    super.componentWillMount()
  }

  override def componentDidMount () {
    super.componentDidMount()
    // TODO Нужно объеденить логику MapComponent и MapСontrol! Для LocateControl нужно сразу обе деятельности.
    // TODO leafletElement
    //  .asInstanceOf[LocateControl]
    //  .addTo(context.map)
  }

  def render(): ReactElement = null

}


@js.native
trait LocateControlPropsR extends LocateOptions {

  /**
    * Optional reaction about freshly detected location.
    * Handled automatically inside MapComponent.bindLeafletEvents().
    */
  var onlocationfound: js.Function1[LocationEvent,_] = js.native

}
