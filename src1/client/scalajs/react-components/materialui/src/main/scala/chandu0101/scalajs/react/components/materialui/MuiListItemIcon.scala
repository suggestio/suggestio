package chandu0101.scalajs.react.components.materialui

import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom._
import scala.scalajs.js

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 05.09.18 17:34
  * Description: A simple wrapper to apply List styles to an Icon or SvgIcon.
  */
object MuiListItemIcon {

  val component = JsComponent[js.Object, Children.Varargs, Null](Mui.ListItemIcon)

  def apply(props: MuiListItemIconProps = new MuiListItemIconProps {})(children: VdomNode*) =
    component(props)(children: _*)

}


trait MuiListItemIconProps extends js.Object {
  val classes: js.UndefOr[js.Object] = js.undefined
}
