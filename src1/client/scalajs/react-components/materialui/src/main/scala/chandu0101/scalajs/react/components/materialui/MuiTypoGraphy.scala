package chandu0101.scalajs.react.components.materialui

import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom._
import scala.scalajs.js

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 26.09.18 14:56
  */
object MuiTypoGraphy {

  val component = JsComponent[MuiTypoGraphyProps, Children.Varargs, Null](Mui.Typography)

  def apply(props: MuiTypoGraphyProps = MuiTypoGraphyProps.empty)(children: VdomNode*) =
    component(props)(children: _*)

}


/** Props for [[MuiTypoGraphy]]. */
trait MuiTypoGraphyProps
  extends MuiPropsBase
  with MuiPropsBaseClasses[MuiTypoGraphyClasses]
  with MuiPropsBaseComponent
{
  val align: js.UndefOr[String] = js.undefined
  val color: js.UndefOr[String] = js.undefined
  val gutterBottom: js.UndefOr[Boolean] = js.undefined
  val headlineMapping: js.UndefOr[MuiTypoGraphyHeadlineMapping] = js.undefined
  val noWrap: js.UndefOr[Boolean] = js.undefined
  val paragraph: js.UndefOr[Boolean] = js.undefined
  val variant: js.UndefOr[String] = js.undefined
}
object MuiTypoGraphyProps extends MuiPropsBaseStatic[MuiTypoGraphyProps]


/** Общие поля для [[MuiTypoGraphyProps]] и [[MuiTypoGraphyProps.headlineMapping]]. */
sealed trait MuiTypoGraphyMappings extends js.Object {
  val h1: js.UndefOr[String] = js.undefined
  val h2: js.UndefOr[String] = js.undefined
  val h3: js.UndefOr[String] = js.undefined
  val h4: js.UndefOr[String] = js.undefined
  val h5: js.UndefOr[String] = js.undefined
  val h6: js.UndefOr[String] = js.undefined
  val subtitle1: js.UndefOr[String] = js.undefined
  val body1: js.UndefOr[String] = js.undefined
  val body2: js.UndefOr[String] = js.undefined
}


/** JSON API for [[MuiTypoGraphyProps.classes]]. */
trait MuiTypoGraphyClasses
  extends MuiClassesBase
  with MuiTypoGraphyMappings
{
  val caption: js.UndefOr[String] = js.undefined
  val button: js.UndefOr[String] = js.undefined
  val srOnly: js.UndefOr[String] = js.undefined
  val alignLeft: js.UndefOr[String] = js.undefined
  val alignCenter: js.UndefOr[String] = js.undefined
  val alignRight: js.UndefOr[String] = js.undefined
  val alignJustify: js.UndefOr[String] = js.undefined
  val noWrap: js.UndefOr[String] = js.undefined
  val gutterBottom: js.UndefOr[String] = js.undefined
  val paragraph: js.UndefOr[String] = js.undefined
  val colorInherit: js.UndefOr[String] = js.undefined
  val colorPrimary: js.UndefOr[String] = js.undefined
  val colorSecondary: js.UndefOr[String] = js.undefined
  val colorTextPrimary: js.UndefOr[String] = js.undefined
  val colorTextSecondary: js.UndefOr[String] = js.undefined
  val colorError: js.UndefOr[String] = js.undefined
}


/** Possible variats for [[MuiTypoGraphyProps.align]]. */
object MuiTypoGraphyAligns {
  val inherit = "inherit"
  val left = "left"
  val center = "center"
  val right = "right"
  val justify = "justify"
}


/** Possible values for [[MuiTypoGraphyProps.color]]. */
object MuiTypoGraphyColors {
  val default = "default"
  val error = "error"
  val inherit = "inherit"
  val primary = "primary"
  val secondary = "secondary"
  val textPrimary = "textPrimary"
  val textSecondary = "textSecondary"
}


/** js object for [[MuiTypoGraphyProps.headlineMapping]]. */
trait MuiTypoGraphyHeadlineMapping
  extends MuiTypoGraphyMappings


/** Values for [[MuiTypoGraphyProps.variant]]. */
object MuiTypoGraphyVariants {
  val h1 = "h1"
  val h2 = "h2"
  val h3 = "h3"
  val h4 = "h4"
  val h5 = "h5"
  val h6 = "h6"

  val subtitle1 = "subtitle1"
  val subtitle2 = "subtitle2"

  val body1 = "body1"
  val body2 = "body2"

  val caption = "caption"
  val button = "button"
  val overline = "overline"

  val srOnly = "srOnly"
  val inherit = "inherit"
}

