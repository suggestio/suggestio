package chandu0101.scalajs.react.components.materialui

import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom._

import scala.scalajs.js

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 04.07.19 10:23
  * Description: Fab вынесен за пределы Button начиная с materialui-4.0.
  */
object MuiFab {

  val component = JsComponent[MuiFabProps, Children.Varargs, Null](Mui.Fab)

  def apply(props: MuiFabProps = MuiFabProps.empty)(children: VdomNode*) =
    component(props)(children: _*)

}


trait MuiFabProps
  extends MuiButtonBaseCommonProps
  with MuiPropsBaseComponent
  with MuiPropsBaseClasses[MuiFabClasses]
{
  val color: js.UndefOr[String] = js.undefined
  val disableFocusRipple: js.UndefOr[Boolean] = js.undefined
  val href: js.UndefOr[String] = js.undefined
  /** @see [[MuiButtonBaseSizes]]. */
  val size: js.UndefOr[String] = js.undefined
  val variant: js.UndefOr[String] = js.undefined
}
object MuiFabProps extends MuiPropsBaseStatic[MuiFabProps]


object MuiFabVariants {
  val round = "round"
  val extended = "extended"
}


trait MuiFabClasses extends MuiClassesBase {
  val label: js.UndefOr[String] = js.undefined
  val primary: js.UndefOr[String] = js.undefined
  val secondary: js.UndefOr[String] = js.undefined
  val extended: js.UndefOr[String] = js.undefined
  val focusVisible: js.UndefOr[String] = js.undefined
  val disabled: js.UndefOr[String] = js.undefined
  val colorInherit: js.UndefOr[String] = js.undefined
  val sizeSmall: js.UndefOr[String] = js.undefined
  val sizeMedium: js.UndefOr[String] = js.undefined
}
