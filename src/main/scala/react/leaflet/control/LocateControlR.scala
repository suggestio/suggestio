package react.leaflet.control

import io.suggest.sjs.leaflet.Leaflet
import io.suggest.sjs.leaflet.control.locate.{LocateControl, LocateOptions}
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
  def jsConstructor = js.constructorOf[LocateControlC]
  jsConstructor.contextTypes = MapControlR.contextTypes

  def apply(): LocateControlR = {
    val p = js.Dynamic.literal.asInstanceOf[LocateControlPropsR]
    LocateControlR(p)
  }

}


/** Scala.js-враппер для js-класса [[LocateControlC]]. */
case class LocateControlR(
  override val props: LocateControlPropsR
)
  extends Wrapper0R[LocateOptions, HTMLElement]
{
  override protected def _rawComponent = LocateControlR.jsConstructor
}


/** Рабочий (благодаря jsConstructor) прототип фасада ES6-класса LocateControl. */
@ScalaJSDefined
sealed class LocateControlC(_props: LocateOptions, _ctx: Context)
  extends MapControlR[LocateOptions](_props, _ctx)
{

  override type El_t = LocateControl

  override def componentWillMount(): Unit = {
    leafletElement = Leaflet.control.locate( props )
  }

}


@js.native
trait LocateControlPropsR extends LocateOptions {

}
