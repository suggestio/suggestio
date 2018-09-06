package chandu0101.scalajs.react.components
package materialui

import japgolly.scalajs.react._

import scala.scalajs.js
import scala.scalajs.js.`|`

object MuiCircularProgress {

  val component = JsComponent[js.Object, Children.None, Null](Mui.CircularProgress)

  def apply(props: MuiCircularProgressProps = new MuiCircularProgressProps {}) = {
    component(props)
  }

}


/** JSON для [[MuiCircularProgress]].props. */
trait MuiCircularProgressProps extends js.Object {
  val classes: js.UndefOr[MuiCircularProgressClasses] = js.undefined
  val color: js.UndefOr[String] = js.undefined
  val size: js.UndefOr[Double | String] = js.undefined
  val thickness: js.UndefOr[Double] = js.undefined
  val value: js.UndefOr[Double] = js.undefined
  val variant: js.UndefOr[String] = js.undefined
}

/** JSON для [[MuiCircularProgressProps]].classes. */
trait MuiCircularProgressClasses extends js.Object {
  val root: js.UndefOr[String] = js.undefined
  val static: js.UndefOr[String] = js.undefined
  // TODO ... https://material-ui.com/api/circular-progress/#css-api
}


@js.native
trait MuiCircularProgressM extends js.Object {
  def rotateWrapper(wrapper: Wrapper): Unit = js.native

  def scalePath(path: CircularProgressPath): Unit = js.native
}
