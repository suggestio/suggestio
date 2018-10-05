package chandu0101.scalajs.react.components.materialui

import japgolly.scalajs.react._
import japgolly.scalajs.react.raw._
import scala.scalajs.js

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 05.10.18 15:40
  */
object MuiSnackBarContent {

  val component = JsComponent[MuiSnackBarContentProps, Children.None, Null](Mui.SnackbarContent)

  def apply(props: MuiSnackBarContentProps = MuiSnackBarContentProps.empty) =
    component(props)

}


/** JSON props for [[MuiSnackBarContent.component]] */
trait MuiSnackBarContentProps
  extends MuiPaperPropsBase
  with MuiPropsBaseClasses[MuiSnackBarContentClasses]
{
  val action: js.UndefOr[React.Node] = js.undefined
  val message: js.UndefOr[React.Node] = js.undefined
}
object MuiSnackBarContentProps extends MuiPropsBaseStatic[MuiSnackBarContentProps]



/** JSON css classes for [[MuiSnackBarContentProps.classes]]. */
trait MuiSnackBarContentClasses extends MuiClassesBase {
  val action: js.UndefOr[String] = js.undefined
  val message: js.UndefOr[String] = js.undefined
  // TODO Inherit MuiPaperClasses?
}

