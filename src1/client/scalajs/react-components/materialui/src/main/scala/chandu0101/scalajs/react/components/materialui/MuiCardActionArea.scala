package chandu0101.scalajs.react.components.materialui

import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom._
import scala.scalajs.js


object MuiCardActionArea {

  val component = JsComponent[MuiCardActionAreaProps, Children.Varargs, Null](Mui.CardActionArea)

  def apply(props: MuiCardActionAreaProps = MuiCardActionAreaProps.empty)(children: VdomNode*) =
    component(props)(children: _*)

}


trait MuiCardActionAreaProps
  extends MuiPropsBase
  with MuiPropsBaseClasses[MuiCardActionAreaClasses]
object MuiCardActionAreaProps extends MuiPropsBaseStatic[MuiCardActionAreaProps]


trait MuiCardActionAreaClasses extends MuiClassesBase {
  val focusVisible: js.UndefOr[String] = js.undefined
  val focusHighlight: js.UndefOr[String] = js.undefined
}
