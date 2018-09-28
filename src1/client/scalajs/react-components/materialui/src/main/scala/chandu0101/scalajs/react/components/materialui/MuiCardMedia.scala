package chandu0101.scalajs.react.components
package materialui

import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom._
import scala.scalajs.js


object MuiCardMedia {

  val component = JsComponent[MuiCardMediaProps, Children.Varargs, Null](Mui.CardMedia)

  def apply(props: MuiCardMediaProps = MuiCardMediaProps.empty)(children: VdomNode*) =
    component(props)(children: _*)

}


trait MuiCardMediaProps
  extends MuiPropsBase
  with MuiPropsBaseClasses[MuiCardMediaClasses]
  with MuiPropsBaseComponent
{
  val image: js.UndefOr[String] = js.undefined
  val src: js.UndefOr[String] = js.undefined
}
object MuiCardMediaProps extends MuiPropsBaseStatic[MuiCardMediaProps]


trait MuiCardMediaClasses extends MuiClassesBase {
  val media: js.UndefOr[String] = js.undefined
}
