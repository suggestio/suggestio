package chandu0101.scalajs.react.components.materialui

import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.VdomNode

import scala.scalajs.js
import scala.scalajs.js.|

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 07.09.18 17:51
  */
object MuiInputAdornment {

  val component = JsComponent[MuiInputAdornmentProps, Children.Varargs, Null](Mui.InputAdornment)

  def apply(props: MuiInputAdornmentProps = MuiInputAdornmentProps.empty)(children: VdomNode*) =
    component(props)(children: _*)

}


trait MuiInputAdornmentProps extends MuiPropsBase {
  val classes: js.UndefOr[MuiInputAdornmentClasses] = js.undefined
  val component: js.UndefOr[String | js.Function | js.Object] = js.undefined
  val disableTypography: js.UndefOr[Boolean] = js.undefined
  val position: js.UndefOr[String] = js.undefined
}
object MuiInputAdornmentProps extends MuiPropsBaseStatic[MuiInputAdornmentProps]


trait MuiInputAdornmentClasses extends js.Object {
  val root: js.UndefOr[String] = js.undefined
  val positionStart: js.UndefOr[String] = js.undefined
  val positionEnd: js.UndefOr[String] = js.undefined
}


object MuiInputAdornmentPositions {
  val start = "start"
  val end = "end"
}
