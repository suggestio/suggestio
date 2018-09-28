package chandu0101.scalajs.react.components
package materialui

import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom._


object MuiCardContent {

  val component = JsComponent[MuiCardContentProps, Children.Varargs, Null](Mui.CardContent)

  def apply(props: MuiCardContentProps = MuiCardContentProps.empty)(children: VdomNode*) =
    component(props)(children: _*)

}


trait MuiCardContentProps
  extends MuiPropsBase
  with MuiPropsBaseClasses[MuiCardContentClasses]
  with MuiPropsBaseComponent
object MuiCardContentProps extends MuiPropsBaseStatic[MuiCardContentProps]


trait MuiCardContentClasses extends MuiClassesBase
