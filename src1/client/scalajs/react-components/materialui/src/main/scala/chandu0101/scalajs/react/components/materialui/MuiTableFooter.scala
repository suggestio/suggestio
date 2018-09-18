package chandu0101.scalajs.react.components
package materialui

import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom._
import scala.scalajs.js


object MuiTableFooter {

  val component = JsComponent[js.Object, Children.Varargs, Null](Mui.TableFooter)

  def apply(props: MuiTableFooterProps = MuiTableFooterProps.empty)(children: VdomNode*) =
    component(props)(children: _*)

}


trait MuiTableFooterProps
  extends MuiPropsBase
  with MuiPropsBaseClasses[MuiTableFooterClasses]
  with MuiPropsBaseComponent
object MuiTableFooterProps extends MuiPropsBaseStatic[MuiTableFooterProps]


trait MuiTableFooterClasses
  extends MuiClassesBase
