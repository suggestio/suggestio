package chandu0101.scalajs.react.components
package materialui

import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom._
import scala.scalajs.js
import scala.scalajs.js.`|`

    
object MuiAvatar {

  val component = JsComponent[js.Object, Children.Varargs, Null](Mui.Avatar)

  def apply(props: MuiAvatarProps)(children: VdomElement*) =
    component(props)(children: _*)

}


/** JSON for [[MuiAvatar]] props. */
trait MuiAvatarProps extends MuiPropsBase {
  val alt: js.UndefOr[String] = js.undefined
  val classes: js.UndefOr[MuiAvatarClasses] = js.undefined
  val component: js.UndefOr[String | js.Function | js.Object] = js.undefined
  val imgProps: js.UndefOr[js.Object] = js.undefined
  val sizes: js.UndefOr[String] = js.undefined
  val src: js.UndefOr[String] = js.undefined
  val srcSet: js.UndefOr[String] = js.undefined
}
object MuiAvatarProps extends MuiPropsBaseStatic[MuiAvatarProps]


/** JSON for [[MuiAvatarProps.classes]]. */
trait MuiAvatarClasses extends js.Object {
  val root: js.UndefOr[String] = js.undefined
  val colorDefault: js.UndefOr[String] = js.undefined
  val img: js.UndefOr[String] = js.undefined
}

