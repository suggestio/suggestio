package react.leaflet.control

import io.suggest.sjs.leaflet.control.locate.{LocateControl, LocateOptions}
import react.leaflet.Context
import japgolly.scalajs.react.{JsComponent, Children}

import scala.scalajs.js

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

  val component = JsComponent[LocateControlPropsR, Children.None, Null]( jsConstructor )

  def apply(props: LocateControlPropsR = new LocateControlPropsR{} ) = component( props )

}


/** Рабочий (благодаря jsConstructor) прототип фасада ES6-класса LocateControl. */
sealed class LocateControlC(_props: LocateOptions, _ctx: Context)
  extends MapControlR[LocateOptions](_props, _ctx)
{

  override type El_t = LocateControl

  override def componentWillMount(): Unit = {
    leafletElement = new LocateControl(props)
  }

}


trait LocateControlPropsR extends LocateOptions {
}
