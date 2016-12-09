package react.leaflet.control

import io.suggest.sjs.leaflet.Leaflet
import io.suggest.sjs.leaflet.control.locate.LocateOptions
import org.scalajs.dom.raw.HTMLElement
import react.leaflet.{Context, Wrapper0R}

import scala.scalajs.js
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

}


/** Scala.js-враппер для js-класса [[LocateControl]]. */
case class LocateControlR(
  override val props: LocateOptions = js.undefined.asInstanceOf[LocateOptions]
)
  extends Wrapper0R[LocateOptions, HTMLElement]
{
  override protected def _rawComponent = LocateControlR.jsConstructor
}


/** Рабочий (благодаря jsConstructor) прототип фасада ES6-класса LocateControl. */
@ScalaJSDefined
sealed class LocateControl(props: LocateOptions, context: Context)
  extends MapControlR[LocateOptions](props, context)
{

  override def componentWillMount(): Unit = {
    leafletElement = Leaflet.control.locate( props )
  }

}
