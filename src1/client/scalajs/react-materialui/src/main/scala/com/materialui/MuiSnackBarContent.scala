package com.materialui

import japgolly.scalajs.react._
import org.scalajs.dom

import scala.scalajs.js

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 05.10.18 15:40
  */
object MuiSnackBarContent {

  val component = JsForwardRefComponent[MuiSnackBarContentProps, Children.None, dom.html.Div]( Mui.SnackbarContent )

  final def apply(props: MuiSnackBarContentProps = MuiPropsBaseStatic.empty) =
    component(props)

}


/** Props for [[MuiSnackBarContent.component]] */
trait MuiSnackBarContentProps
  extends MuiPaperPropsBase
  with MuiPropsBaseClasses[MuiSnackBarContentClasses]
{
  val action: js.UndefOr[raw.React.Node] = js.undefined
  val message: js.UndefOr[raw.React.Node] = js.undefined
}



/** CSS classes for [[MuiSnackBarContentProps.classes]]. */
trait MuiSnackBarContentClasses extends MuiClassesBase {
  val action: js.UndefOr[String] = js.undefined
  val message: js.UndefOr[String] = js.undefined
  // TODO Inherit MuiPaperClasses?
}

